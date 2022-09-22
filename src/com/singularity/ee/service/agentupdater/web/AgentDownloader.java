package com.singularity.ee.service.agentupdater.web;

import com.singularity.ee.service.agentupdater.JavaAgentVersion;
import com.singularity.ee.service.agentupdater.json.AgentDownloadListing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;

public class AgentDownloader {
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.agentupdater.web.AgentDownloader");
    private AgentDownloadListing agentDownloadListing = null;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AgentDownloader( String type, JavaAgentVersion javaAgentVersion) {
        this(type, javaAgentVersion.getVersion());
    }
    public AgentDownloader(String type, String version) {
        agentDownloadListing = getListOfAgents(type, version);
    }

    private AgentDownloadListing getListOfAgents( String type, String version ) {
        logger.info(String.format("Fetching list of agents available for download of type: %s and version: %s",type,version));
        try {
            URL url = new URL( String.format("https://download.appdynamics.com/download/downloadfile/?version=%s&apm=%s&format=json", version, type) );
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("GET");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) { response.append(inputLine); response.append("\n"); }
            in.close();
            if( connection.getResponseCode() != 200 )
                logger.warn("Response: "+ connection.getResponseMessage());
            logger.debug(String.format("Response from download list request: '%s'", response.toString()));
            return gson.fromJson( response.toString(), AgentDownloadListing.class);
        } catch (MalformedURLException e) {
            logger.error(String.format("Malformed URL Exception: %s",e),e);
        } catch (IOException ioException) {
            logger.error(String.format("IO Exception: %s",ioException),ioException);
        }

        return null;
    }

    public ZipFileWithVersion getAgentZipFile(String downloadURL) throws IOException {
        FileOutputStream outputStream = null;
        try {
            AgentDownloadListing.DownloadDetails downloadDetails = this.agentDownloadListing.getBestAgent();
            if( downloadDetails == null) {
                logger.error("No suitable download found for update");
                return null;
            }
            File tempFile = File.createTempFile("temp-agent-download", ".zip");
            outputStream = new FileOutputStream(tempFile);
            URL url = new URL("https://download-files.appdynamics.com/"+downloadDetails.s3_path);
            if( downloadURL != null && downloadURL.length()>0 ) {
                if( !downloadURL.endsWith("/") ) downloadURL = downloadURL + "/";
                url = new URL(downloadURL + downloadDetails.filename);
            }
            logger.info("downloading "+ url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = connection.getHeaderField("Location");
                    String cookies = connection.getHeaderField("Set-Cookie");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestProperty("Cookie", cookies);
                }
            }
            outputStream.getChannel().transferFrom(Channels.newChannel(connection.getInputStream()), 0, Long.MAX_VALUE);
            logger.info("Temp file downloaded to: "+ tempFile.getAbsolutePath() + "New Version is: "+ downloadDetails.version);
            return new ZipFileWithVersion(tempFile, downloadDetails.version);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {}
        }
    }
}
