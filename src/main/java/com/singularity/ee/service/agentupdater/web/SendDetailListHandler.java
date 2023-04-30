package com.singularity.ee.service.agentupdater.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;
import com.singularity.ee.service.agentupdater.json.JavaAgentVersion;
import com.singularity.ee.service.agentupdater.json.AgentDownloadListing;
import com.singularity.ee.service.agentupdater.json.DownloadDetails;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendDetailListHandler implements HttpHandler {
    //private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.agentupdater.web.SendDetailListHandler");
    private static Pattern versionQueryParameter = Pattern.compile("version=(?<version>.*)\\&apm=.*");
    private List<DownloadDetails> downloadDetailsList;
    private Gson gson = new GsonBuilder().create();

    public SendDetailListHandler(List<DownloadDetails> downloadDetailsList) {
        this.downloadDetailsList = downloadDetailsList;
    }

    public void handle(HttpExchange httpExchange) throws IOException {
        String version = getVersion(httpExchange);
        System.out.println(String.format("HTTP Request: %s version: %s", httpExchange.getRequestURI(), version ));
        int responseCode = 200;
        String response = "";

        if( version == null ) {
            responseCode = 500;
            response = "Error reading version parameter";
        } else {
            JavaAgentVersion javaAgentVersion = new JavaAgentVersion(version);
            AgentDownloadListing agentDownloadListing = new AgentDownloadListing();
            agentDownloadListing.results = new ArrayList<>();
            for (DownloadDetails downloadDetails : downloadDetailsList) {
                System.out.println("trying: " + downloadDetails.version);
                if (downloadDetails.matches(javaAgentVersion)) {
                    System.out.println("Adding agent download to list: " + downloadDetails.filename);
                    agentDownloadListing.results.add(downloadDetails);
                }
            }
            agentDownloadListing.count = agentDownloadListing.results.size();
            response = gson.toJson(agentDownloadListing);
        }
        System.out.println(String.format("Built Response: '%s'",response));
        httpExchange.setAttribute("Content-Type", "application/json");
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(responseCode, response.length());
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
        httpExchange.getRequestBody().close();
    }

    private String getVersion(HttpExchange httpExchange) {
        Matcher matcher = versionQueryParameter.matcher(httpExchange.getRequestURI().getQuery());
        String version = null;
        if( matcher.find() ) version = matcher.group("version");
        return version;
    }

}
