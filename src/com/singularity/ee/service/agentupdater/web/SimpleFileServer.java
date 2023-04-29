package com.singularity.ee.service.agentupdater.web;

import com.singularity.ee.service.agentupdater.json.DownloadDetails;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleFileServer {

    private List<DownloadDetails> downloadDetailsList;

    public SimpleFileServer( int port, File directory ) {
        this.downloadDetailsList = new ArrayList<>();
        try {
            InetSocketAddress address = new InetSocketAddress(port);
            HttpServer httpServer = HttpServer.create(address, 0);
            int cnt=0;
            for( File file : directory.listFiles() ) {
                if(file.isFile()) {
                    httpServer.createContext("/"+file.getName(), new SendFileHandler(file));
                    System.out.println(String.format("Adding file: /%s",file.getName()));
                    this.downloadDetailsList.add( new DownloadDetails(cnt++,file) );
                }
            }
            httpServer.createContext("/download/downloadfile/", new SendDetailListHandler(this.downloadDetailsList));
            httpServer.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
            httpServer.start();
            System.out.println("Server Started listening on "+ address.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void main( String... args) {
        int port = Integer.parseInt(System.getProperty("server-port", "8000"));
        String dir = System.getProperty("server-directory", ".");
        System.out.println(String.format("Setting port to: %d, and directory to %s, do override set with property -Dserver-port=### and -Dserver-directory=/somewhere",port,dir));
        SimpleFileServer simpleFileServer = new SimpleFileServer(port, new File(dir));
    }
}
