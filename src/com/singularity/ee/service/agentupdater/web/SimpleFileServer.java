package com.singularity.ee.service.agentupdater.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleFileServer {

    public SimpleFileServer( int port, File directory ) {
        try {
            InetSocketAddress address = new InetSocketAddress(port);
            HttpServer httpServer = HttpServer.create(address, 0);
            for( File file : directory.listFiles() ) {
                if(file.isFile()) {
                    httpServer.createContext("/"+file.getName(), new SendFileHandler(file));
                    System.out.println(String.format("Adding file: /%s",file.getName()));
                }
            }
            httpServer.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
            httpServer.start();
            System.out.println("Server Started listening on "+ address.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public class SendFileHandler implements HttpHandler {
        private File file;
        public SendFileHandler( File file ) {
            this.file=file;
        }

        public void handle( HttpExchange httpExchange ) throws IOException {
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

    public static void main( String... args) {
        int port = Integer.parseInt(System.getProperty("server-port", "8000"));
        String dir = System.getProperty("server-directory", ".");
        System.out.println(String.format("Setting port to: %d, and directory to %s, do override set with property -Dserver-port=### and -Dserver-directory=/somewhere",port,dir));
        SimpleFileServer simpleFileServer = new SimpleFileServer(port, new File(dir));
    }
}
