package com.singularity.ee.service.agentupdater.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

public class SendFileHandler implements HttpHandler {
    private File file;

    public SendFileHandler(File file) {
        this.file = file;
    }

    public void handle(HttpExchange httpExchange) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        InputStream inputStream = new FileInputStream(this.file);
        while ((length = inputStream.read(buffer)) != -1) {
            response.write(buffer, 0, length);
        }
        httpExchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + this.file.getName());
        httpExchange.setAttribute("Content-Type", "application/zip");
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(200, response.size());
        outputStream.write(response.toByteArray());
        outputStream.flush();
        outputStream.close();
        httpExchange.getRequestBody().close();
    }
}
