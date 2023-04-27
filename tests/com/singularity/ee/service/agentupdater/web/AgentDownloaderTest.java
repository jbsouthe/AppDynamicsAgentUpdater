package com.singularity.ee.service.agentupdater.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.singularity.ee.service.agentupdater.json.AgentDownloadListing;
import com.singularity.ee.service.agentupdater.json.DownloadDetails;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;

public class AgentDownloaderTest extends TestCase {

    public AgentDownloaderTest() {

    }

    @Test
    public void testAgentDownloaderAppDDownloads() throws IOException {
        URL url = new URL( String.format("https://download.appdynamics.com/download/downloadfile/?version=%s&apm=%s&format=json", "22.8.0", "java-jdk8") );
        System.out.println(String.format("GET Request: '%s'",url));
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
            System.out.println("Response: "+ connection.getResponseMessage());
        System.out.println(String.format("Response from dowload list request: '%s'", response.toString()));
        AgentDownloadListing agentDownloadListing = gson.fromJson(response.toString(), AgentDownloadListing.class);

        DownloadDetails downloadDetails = agentDownloadListing.getBestAgent();
        File tempFile = File.createTempFile("temp-agent-download", ".zip");
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        url = new URL(downloadDetails.download_path);
        connection = (HttpURLConnection) url.openConnection();
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = connection.getHeaderField("Location");
                String cookies = connection.getHeaderField("Set-Cookie");
                System.out.println("New URL: "+ newUrl +" cookies: "+ cookies);
                connection = (HttpURLConnection) new URL(newUrl).openConnection();
                connection.setRequestProperty("Cookie", cookies);
            }
        }
        outputStream.getChannel().transferFrom(Channels.newChannel(connection.getInputStream()), 0, Long.MAX_VALUE);
        System.out.println("File downloaded to: '"+ tempFile.getAbsolutePath() +"' Size: "+ tempFile.length());
    }

}