package com.singularity.ee.service.agentupdater;

import com.singularity.ee.agent.appagent.kernel.spi.IDynamicService;
import com.singularity.ee.service.agentupdater.json.JavaAgentVersion;
import com.singularity.ee.service.agentupdater.json.LockFile;
import com.singularity.ee.service.agentupdater.web.AgentDownloader;
import com.singularity.ee.agent.appagent.kernel.ServiceComponent;
import com.singularity.ee.agent.appagent.kernel.spi.IServiceContext;
import com.singularity.ee.agent.appagent.services.bciengine.JavaAgentManifest;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;
import com.singularity.ee.service.agentupdater.web.ZipFileWithVersion;
import com.singularity.ee.util.io.FileUtil;
import com.singularity.ee.util.javaspecific.threads.IAgentRunnable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CheckForAgentUpgradeRequestTask implements IAgentRunnable {
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.agentupdater.CheckForAgentUpgradeRequestTask");
    private static final String LOCK_FILE_LOCATION = "external-services/agent-updater/lockFile.json";
    private IDynamicService agentService;
    private AgentNodeProperties agentNodeProperties;
    private ServiceComponent serviceComponent;
    private IServiceContext serviceContext;

    public CheckForAgentUpgradeRequestTask(IDynamicService agentService, AgentNodeProperties agentNodeProperties, ServiceComponent serviceComponent, IServiceContext iServiceContext) {
        this.agentNodeProperties=agentNodeProperties;
        this.agentService=agentService;
        this.serviceComponent=serviceComponent;
        this.serviceContext=iServiceContext;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        logger.info("Running the task to check versions and upgrade if needed");
        //1. check current version against preferred and min>x<max version
        JavaAgentVersion currentVersion = agentNodeProperties.getCurrentVersion();
        if( currentVersion == null ) currentVersion = new JavaAgentVersion(getCurrentVersionFromFile());
        JavaAgentVersion newVersion = null;
        agentNodeProperties.updateProperty("agent.upgrader.version.current", currentVersion.getVersion());
        if ( agentNodeProperties.isPreferredVersionSet()) { //preferred version node property will tell us to upgrade/downgrade to a version
            newVersion = agentNodeProperties.getPreferredVersion();
            if( currentVersion.compareTo( newVersion ) == 0 ) return; //done
        } else if(agentNodeProperties.isMinVersionSet() && currentVersion.compareTo(agentNodeProperties.getMinVersion()) < 0 ) {
            newVersion = agentNodeProperties.getMinVersion();
        } else if(agentNodeProperties.isMaxVersionSet() && currentVersion.compareTo(agentNodeProperties.getMaxVersion()) > 0 ) {
            newVersion = agentNodeProperties.getMaxVersion();
        }
        if( newVersion != null ) upgradeJavaAgent( currentVersion, newVersion );
    }

    private void upgradeJavaAgent(JavaAgentVersion currentVersion, JavaAgentVersion newVersion) {
        logger.info(String.format("Upgrade Java Agent from %s to %s",currentVersion.getVersion(), newVersion.getVersion()));
        /*2. if upgrade needed, then
            a. download newest/preferred version of agent
            -. check lock file, create lock file, verify lock file
            b. copy root controller-info.xml to backup
            c. unzip agent into current directory
            d. copy backup root config files to root
            e. copy customized config files to new version dir
            f. update current version and send custom event alerting that restart is needed
            z. remove lock file
         */
        LockFile lockFile = null;
        try {
            AgentDownloader agentDownloader = new AgentDownloader("java-jdk8", newVersion.getVersion(), agentNodeProperties);
            ZipFileWithVersion zipFileWithVersion = agentDownloader.getAgentZipFile();
            if( zipFileWithVersion == null ) {
                logger.warn("No Agent Found for version "+ newVersion);
                sendInfoEvent(String.format("Agent Updater can not find an agent for version '%s', please make sure it is available",newVersion));
                return;
            }
            lockFile = LockFile.getLockFile( serviceContext.getInstallDir() + File.pathSeparator + LOCK_FILE_LOCATION);
            if( lockFile != null && !lockFile.isThisMe() ) {
                boolean continueAnyway = false;
                if( lockFile.age() > 5*60000 ) { //if older than five minutes it is most likely stale
                    continueAnyway = true;
                }
                sendInfoEvent(String.format("Agent Updater attempted to get lock file, found an existing one '%s' will continue? %s", lockFile, continueAnyway));
                if( continueAnyway ) {
                    lockFile.delete();
                    LockFile.getLockFile(serviceContext.getInstallDir() + File.pathSeparator + LOCK_FILE_LOCATION);
                    if ( lockFile == null || !lockFile.isThisMe()) {
                        continueAnyway = false;
                        sendInfoEvent(String.format("Agent Updater attempted to get lock file a second time and still found an existing one '%s' will continue? %s", lockFile, continueAnyway));
                    }
                    lockFile = null;
                } else {
                    lockFile = null;
                }
            }
            if( lockFile == null ) { //all our attempts failed for some reason, time to bail
                logger.warn("Was not able to get a lock file, let's stop and try again in the next cycle");
                sendInfoEvent("Agent Updater was not able to obtain the lock file, will stop and let the owner of the lock upgrade");
            }
            newVersion = zipFileWithVersion.javaAgentVersion;
            logger.info(String.format("installDir: %s BaseConfDir: %s ConfDir: %s AgentRuntimeDir: %s RuntimeConfDir: %s",
                    serviceContext.getInstallDir(), serviceContext.getBaseConfDir(), serviceContext.getConfDir(),
                    serviceContext.getAgentRuntimeDir(), serviceContext.getRuntimeConfDir()));
            File backupControllerInfoFile = File.createTempFile("controller-info", ".xml");
            File rootControllerInfoFile = new File(serviceContext.getBaseConfDir()+"/controller-info.xml");
            copyFiles(rootControllerInfoFile, backupControllerInfoFile);
            unzip(zipFileWithVersion.zipFile, serviceContext.getInstallDir()+"/..");
            copyFiles(backupControllerInfoFile, rootControllerInfoFile);
            if( ! serviceContext.getConfDir().equals(serviceContext.getRuntimeConfDir()) ) {
                sendInfoEvent("Agent is using a custom runtime config directory, we will not modify these files: "+ serviceContext.getRuntimeConfDir());
            }
            copyFiles(new File(serviceContext.getConfDir()), new File(serviceContext.getInstallDir() +"/../"+ newVersion.getDirectory() + "/conf"));
            copyFiles(new File(serviceContext.getInstallDir()  + "/external-services/agent-updater"),
                    new File(serviceContext.getInstallDir()+"/../"+ newVersion.getDirectory() + "/external-services/agent-updater"));
            copyFiles(new File(serviceContext.getInstallDir()  + "/sdk-plugins"),
                    new File(serviceContext.getInstallDir()+"/../"+ newVersion.getDirectory() + "/sdk-plugins"));
            agentNodeProperties.updateProperty("agent.upgrader.version.preferred", newVersion.getVersion());
            agentNodeProperties.updateProperty("agent.upgrader.version.current", newVersion.getVersion());
            sendInfoEvent(String.format("Agent Updater has staged an upgrade from %s to %s, please restart the application for this to take effect", currentVersion.getVersion(), newVersion.getVersion()));
        } catch (IOException ioException) {
            logger.error("Exception while trying to upgrade agent: "+ ioException, ioException);
        } finally {
            if( lockFile != null ) {
                logger.debug(String.format("Removing lock file: %s", lockFile));
                lockFile.delete();
            }
        }
    }

    private void sendInfoEvent(String message) {
        sendInfoEvent(message, new HashMap());
    }

    private void sendInfoEvent(String message, Map map) {
        logger.info("Sending Custom INFO Event with message: "+ message);
        serviceComponent.getEventHandler().publishInfoEvent(message, map);
    }

    private void copyFiles(File sourceLocation, File targetLocation) throws IOException {
        logger.debug(String.format("copyFiles '%s' -> '%s'",sourceLocation.getAbsolutePath(),targetLocation.getAbsolutePath()));
        if( sourceLocation.isDirectory() ) {
            if( !targetLocation.exists() ) targetLocation.mkdir();
            for( String child : sourceLocation.list() )
                copyFiles( new File(sourceLocation, child), new File(targetLocation, child));
        } else {
            FileUtil.copyFile(sourceLocation, targetLocation);
        }
    }

    private void unzip(File zipFile, String targetDir) throws IOException {
        logger.info(String.format("Unzipping agent package from '%s' to '%s'", zipFile.getAbsolutePath(), targetDir.replaceAll(" ", "\\ ")));
        Path destination = Paths.get(targetDir.replaceAll(" ", "\\ ")).normalize();
        if( !Files.exists(destination) ) { Files.createDirectories(destination); }
        FileInputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry zipEntry = zis.getNextEntry();
        while( zipEntry != null) {
            Path path = destination.resolve(zipEntry.getName()).normalize();
            if(!path.startsWith(destination)) {
                logger.error("Invalid ZIP Entry! "+ zipEntry.getName());
                continue;
            }
            if(zipEntry.isDirectory()) {
                Files.createDirectories(path);
            }else{
                BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path));
                byte[] bytes = new byte[1024];
                int length;
                while (( length = zis.read(bytes)) >= 0) {
                    bos.write(bytes, 0, length);
                }
                bos.close();
            }
            zis.closeEntry();
            zipEntry = zis.getNextEntry();
        }
        zis.close();
        fis.close();
    }

    private String getCurrentVersionFromFile() {
        JavaAgentManifest javaAgentManifest = JavaAgentManifest.parseManifest(serviceContext.getInstallDir());
        return javaAgentManifest.getJavaAgentVersion();
    }
    
}
