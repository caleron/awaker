package com.awaker.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

public class WebContentServer implements HttpHandler {

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/", new WebContentServer());
            server.setExecutor(Executors.newSingleThreadExecutor()); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String root = "./web";
        URI uri = httpExchange.getRequestURI();

        String path = uri.getPath();
        File file = new File(root + path).getCanonicalFile();

        if (file.isDirectory()) {
            File indexFile = new File(root + path + "/index.html");
            if (indexFile.exists()) {
                sendFile(httpExchange, path + "/index.html", indexFile);
            } else {
                System.out.println("did not found " + path);
                send404(httpExchange);
            }
        } else if (!file.isFile()) {
            // Object does not exist or is not a file: reject with 404 error.
            System.out.println("did not found " + path);
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
