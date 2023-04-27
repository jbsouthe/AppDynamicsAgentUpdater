package com.singularity.ee.service.agentupdater.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.singularity.ee.service.agentupdater.json.JavaAgentVersion;
import com.singularity.ee.service.agentupdater.json.AgentDownloadListing;
import com.singularity.ee.service.agentupdater.json.DownloadDetails;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.List;

public class SendDetailListHandler implements HttpHandler {
    private List<DownloadDetails> downloadDetailsList;
    private Gson gson = new GsonBuilder().create();

    public SendDetailListHandler(List<DownloadDetails> downloadDetailsList) {
        this.downloadDetailsList = downloadDetailsList;
    }

    public void handle(HttpExchange httpExchange) throws IOException {
        int responseCode = 200;
        String response = "";
        String version = (String) httpExchange.getAttribute("version");
        if( version == null ) {
            responseCode = 500;
            response = "Error reading version parameter";
        } else {
            JavaAgentVersion javaAgentVersion = new JavaAgentVersion(version);
            AgentDownloadListing agentDownloadListing = new AgentDownloadListing();
            for (DownloadDetails downloadDetails : downloadDetailsList)
                if (downloadDetails.matches(javaAgentVersion)) agentDownloadListing.results.add(downloadDetails);
            agentDownloadListing.count = agentDownloadListing.results.size();
            response = gson.toJson(agentDownloadListing);
        }
        httpExchange.setAttribute("Content-Type", "application/json");
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(responseCode, response.length());
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
        httpExchange.getRequestBody().close();
    }
}
