package com.awaker.server;

import com.awaker.config.PortConfig;
import com.awaker.global.UserActivityCenter;
import com.awaker.util.Log;
import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.concurrent.Executors;

public class WebContentServer implements HttpHandler {

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PortConfig.WEBCONTENT_PORT), 0);
            server.createContext("/", new WebContentServer());
            server.setExecutor(Executors.newSingleThreadExecutor()); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Startet einen https-Server. http://stackoverflow.com/questions/2308479/simple-java-https-server
     * <p>
     * Zertifikat momentan mit keytool erstellt und wird nicht von Browsern akzeptiert. Künftig von
     * https://www.startssl.com/ Zertifikat holen.
     */
    public static void startSecure() {
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(PortConfig.WEBCONTENT_SECURE_PORT), 0);

            SSLContext sslContext = SSLContext.getInstance("TLS");

            String keystorePassword = "ku76??doRa99kratos";
            String keyPassword = "brat+//StorchsenF";
            String keyName = "awaker_keys";
            String fileName = "keys/awaker_store.jks";

            KeyStore ks = KeyStore.getInstance("JKS");

            FileInputStream fis = new FileInputStream(fileName);
            ks.load(fis, keystorePassword.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // get the default parameters
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            server.createContext("/", new WebContentServer());
            server.setExecutor(Executors.newSingleThreadExecutor()); // creates a default executor
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        UserActivityCenter.reportActivity(this);

        String root = "./web";
        URI uri = httpExchange.getRequestURI();

        String path = uri.getPath();
        File file = new File(root + path).getCanonicalFile();

        if (file.isDirectory()) {
            File indexFile = new File(root + path + "/index.html");
            if (indexFile.exists()) {
                sendFile(httpExchange, path + "/index.html", indexFile);
            } else {
                Log.message("did not found " + path);
                send404(httpExchange);
            }
        } else if (!file.isFile()) {
            // Object does not exist or is not a file: reject with 404 error.
            Log.message("did not found " + path);
            send404(httpExchange);
        } else {
            // Object exists and is a file: accept with response code 200.
            sendFile(httpExchange, path, file);
        }
    }

    private static void send404(HttpExchange httpExchange) throws IOException {
        String response = "404 (Not Found)\n";
        httpExchange.sendResponseHeaders(404, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }


    private static void sendFile(HttpExchange httpExchange, String path, File file) throws IOException {
        String mime = "text/html";
        if (path.substring(path.length() - 3).equals(".js")) mime = "application/javascript";
        if (path.substring(path.length() - 3).equals("css")) mime = "text/css";

        Headers h = httpExchange.getResponseHeaders();
        h.set("Content-Type", mime);
        httpExchange.sendResponseHeaders(200, 0);

        OutputStream os = httpExchange.getResponseBody();
        FileInputStream fs = new FileInputStream(file);

        final byte[] buffer = new byte[65536];
        int count;

        while ((count = fs.read(buffer)) >= 0) {
            os.write(buffer, 0, count);
        }
        fs.close();
        os.close();
    }

}
