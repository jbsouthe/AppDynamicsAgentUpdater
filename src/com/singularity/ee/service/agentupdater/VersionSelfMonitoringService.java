package com.singularity.ee.service.agentupdater;

import com.singularity.ee.agent.appagent.kernel.AgentKernel;
import com.singularity.ee.agent.appagent.kernel.LifeCycleManager;
import com.singularity.ee.agent.appagent.kernel.ServiceComponent;
import com.singularity.ee.agent.appagent.kernel.spi.IAgentService;
import com.singularity.ee.agent.appagent.kernel.spi.IDynamicService;
import com.singularity.ee.agent.appagent.kernel.spi.IDynamicServiceManager;
import com.singularity.ee.agent.appagent.kernel.spi.IServiceContext;
import com.singularity.ee.agent.appagent.kernel.spi.data.IServiceConfig;
import com.singularity.ee.agent.appagent.kernel.spi.exception.ConfigException;
import com.singularity.ee.agent.appagent.kernel.spi.exception.ServiceStartException;
import com.singularity.ee.agent.appagent.kernel.spi.exception.ServiceStopException;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;
import com.singularity.ee.util.javaspecific.threads.IAgentRunnable;
import com.singularity.ee.util.spi.AgentTimeUnit;
import com.singularity.ee.util.spi.IAgentScheduledExecutorService;
import com.singularity.ee.util.spi.IAgentScheduledFuture;

public class VersionSelfMonitoringService implements IDynamicService {

    private AgentNodeProperties agentNodeProperties = new AgentNodeProperties();
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.agentupdater.VersionSelfMonitoringService");
    private boolean isServiceStarted = false;
    private IAgentScheduledFuture scheduledTaskFuture;
    private final ServiceComponent serviceComponent = LifeCycleManager.getInjector();
    private long taskInitialDelay=45;
    private long taskInterval=180;
    private IAgentScheduledExecutorService scheduler;
    private IServiceContext iServiceContext;
    private IDynamicServiceManager dynamicServiceManager;

    public VersionSelfMonitoringService() {
        logger.info("Initializing Agent Updater Service");
    }

    public VersionSelfMonitoringService(AgentNodeProperties agentNodeProperties, long taskInitialDelay, long taskInterval) {
        this();
        this.agentNodeProperties = agentNodeProperties;
        this.taskInitialDelay = taskInitialDelay;
        this.taskInterval = taskInterval;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void setServiceContext(IServiceContext iServiceContext) {
        logger.info(String.format("Setting Service Context to %s",iServiceContext));
        this.iServiceContext=iServiceContext;
        this.scheduler = iServiceContext.getAgentScheduler();
    }

    @Override
    public void configure(IServiceConfig iServiceConfig) throws ConfigException {

    }

    @Override
    public void start() throws ServiceStartException {
        new AgentNodePropertyListener(this);
        if(!agentNodeProperties.isEnabled()) {
            logger.info("Service " + this.getName() + " is not enabled.  So not starting this service.  To start it enable the node property agent.upgrader.enabled");
            return;
        }
        if( this.isServiceStarted ) {
            logger.info("Service " + this.getName() + " is already started");
            return;
        }
        if (this.scheduler == null) {
            throw new ServiceStartException("Scheduler is not set, so unable to start the agent self upgrader service");
        }
        if (this.serviceComponent == null) {
            throw new ServiceStartException("Dagger not initialised, so cannot start the agent self upgrader service");
        }
        this.scheduledTaskFuture = this.scheduler.scheduleWithFixedDelay(this.createTask(this.serviceComponent), this.taskInitialDelay, this.taskInterval, AgentTimeUnit.SECONDS);
        this.isServiceStarted = true;
        logger.info("Started " + this.getName() + " with initial delay " + this.taskInitialDelay + ", and with interval " + this.taskInterval + " in Seconds");

    }

    private IAgentRunnable createTask(ServiceComponent serviceComponent) {
        logger.info("Creating Task for agent version upgrade monitoring");
        return new CheckForAgentUpgradeRequestTask( this, this.agentNodeProperties, serviceComponent, iServiceContext);
    }

    @Override
    public void allServicesStarted() {

    }

    @Override
    public void stop() throws ServiceStopException {
        if (!this.isServiceStarted) {
            logger.info("Service " + this.getName() + " not running");
            return;
        }
        if (this.scheduledTaskFuture != null && !this.scheduledTaskFuture.isCancelled() && !this.scheduledTaskFuture.isDone()) {
            this.scheduledTaskFuture.cancel(true);
            this.scheduledTaskFuture = null;
            this.isServiceStarted = false;
        }
    }

    @Override
    public void hotDisable() {
        logger.info("Disabling agent updater service");
        try {
            this.stop();
        }
        catch (ServiceStopException e) {
            logger.error("unable to stop the services", (Throwable)e);
        }
    }

    @Override
    public void hotEnable() {
        logger.info("Enabling agent updater service");
        try {
            this.start();
        }
        catch (ServiceStartException e) {
            logger.error("unable to start the services", (Throwable)e);
        }

    }

    @Override
    public void setDynamicServiceManager(IDynamicServiceManager iDynamicServiceManager) {
        this.dynamicServiceManager = iDynamicServiceManager;
    }

    public IServiceContext getServiceContext() {
        return iServiceContext;
    }

    public AgentNodeProperties getAgentNodeProperties() {
        return agentNodeProperties;
    }
}
