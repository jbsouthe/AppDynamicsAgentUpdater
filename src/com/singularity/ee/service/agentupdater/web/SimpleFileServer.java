package com.singularity.ee.service.agentupdater.web;

import com.singularity.ee.service.agentupdater.json.DownloadDetails;
import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleFileServer {

    private List<DownloadDetails> downloadDetailsList;

    public SimpleFileServer( int port, File directory, Properties props ) {
        try {
            System.out.println("Starting HTTP Server (without SSL) ...");
            InetSocketAddress address = new InetSocketAddress(port);
            HttpServer httpServer = HttpServer.create(address, 0);
            addContexts( httpServer, directory);
            httpServer.setExecutor(
                    new ThreadPoolExecutor(4, Integer.parseInt(props.getProperty("threadPoolMaxSize", "8")),
                    Integer.parseInt(props.getProperty("threadPoolKeepAliveSeconds", "30")), TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(Integer.parseInt(props.getProperty("threadPoolCapacity", "100")))));
            httpServer.start();
            System.out.println("Server Started listening on "+ address.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public SimpleFileServer( Properties props ) { //SSL or some shit
        boolean useSSL = false;
        if( props.getProperty("useSSL","false").equalsIgnoreCase("true") )
            useSSL=true;

        int port = Integer.parseInt(props.getProperty("server-port","8000"));
        String directory = props.getProperty("server-directory", ".");

        try {
            // setup the socket address
            InetSocketAddress address = new InetSocketAddress( port );

            if( useSSL ) {
                System.out.println("Starting HTTPS Server...");
                // initialise the HTTPS server
                HttpsServer httpsServer = HttpsServer.create(address, 0);
                SSLContext sslContext = SSLContext.getInstance(props.getProperty("sslContext", "TLS"));

                // initialise the keystore
                char[] password = props.getProperty("keystorePassword", "password").toCharArray();
                KeyStore ks = KeyStore.getInstance(props.getProperty("keystoreInstance", "JKS"));
                //keytool -genkeypair -keyalg RSA -alias selfsigned -keystore testkey.jks -storepass password -validity 360 -keysize 2048
                FileInputStream fis = new FileInputStream(props.getProperty("keystoreFile", "testkey.jks"));
                ks.load(fis, password);

                // setup the key manager factory
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(props.getProperty("keyManagerFactory", "SunX509"));
                kmf.init(ks, password);

                // setup the trust manager factory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(props.getProperty("trustManagerFactory", "SunX509"));
                tmf.init(ks);

                // setup the HTTPS context and parameters
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    public void configure(HttpsParameters params) {
                        try {
                            // initialise the SSL context
                            SSLContext context = getSSLContext();
                            SSLEngine engine = context.createSSLEngine();
                            params.setCipherSuites(engine.getEnabledCipherSuites());
                            params.setProtocols(engine.getEnabledProtocols());

                            // Set the SSL parameters
                            SSLParameters sslParameters = context.getSupportedSSLParameters();
                            params.setSSLParameters(sslParameters);

                        } catch (Exception ex) {
                            System.out.println("Failed to create HTTPS port "+ ex.getMessage());
                        }
                    }
                });
                addContexts( httpsServer, new File(directory));
                httpsServer.setExecutor(new ThreadPoolExecutor(4, Integer.parseInt(props.getProperty("threadPoolMaxSize", "8")), Integer.parseInt(props.getProperty("threadPoolKeepAliveSeconds", "30")), TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(Integer.parseInt(props.getProperty("threadPoolCapacity", "100")))));
                httpsServer.start();
                System.out.println("Server Started listening on "+ address.toString());
            } else {
                new SimpleFileServer( port, new File(directory), props ); //silly
            }

        } catch (Exception exception) {
            System.out.println("Failed to create web server on port " + props.getProperty("serverPort","8000") + " of localhost; Exception: " + exception.getMessage());
            return;
        }
    }

    private void addContexts(HttpServer httpServer, File directory) throws IOException {
        this.downloadDetailsList = new ArrayList<>();
        int cnt=0;
        for( File file : directory.listFiles() ) {
            if( file.isFile() && file.getName().toLowerCase().endsWith(".zip") ) {
                httpServer.createContext("/"+file.getName(), new SendFileHandler(file));
                System.out.println(String.format("Adding file: /%s",file.getName()));
                this.downloadDetailsList.add( new DownloadDetails(cnt++,file) );
            }
        }
        httpServer.createContext("/download/downloadfile/", new SendDetailListHandler(this.downloadDetailsList));
    }

    public static void main( String... args) {
        if( args.length == 0 ) { //simple mode...
            int port = Integer.parseInt(System.getProperty("server-port", "8000"));
            String dir = System.getProperty("server-directory", ".");
            System.out.println(String.format("Setting port to: %d, and directory to %s, do override set with property -Dserver-port=### and -Dserver-directory=/somewhere", port, dir));
            SimpleFileServer simpleFileServer = new SimpleFileServer(port, new File(dir), new Properties());
        } else { //hard mode, grrrrr
            String configFileName = args[0];
            System.out.println("System starting with configuration: " + configFileName );

            Properties props = new Properties();
            File configFile = new File(configFileName);
            InputStream is = null;
            if( configFile.canRead() ) {
                try {
                    is = new FileInputStream(configFile);
                } catch (FileNotFoundException e) {
                    System.out.println("Config file not found! Exception: "+e);
                }
            }
            if(is == null) {
                System.out.println("UNABLE TO START, no configuration found!");
                System.exit(1);
            }
            try {
                props.load(is);
            } catch (IOException e) {
                System.out.println("Error loading configuration: "+ configFileName +" Exception: "+ e.getMessage());
                System.exit(1);
            }

            SimpleFileServer notSoSimpleFileServer = new SimpleFileServer(props);

        }
    }
}
