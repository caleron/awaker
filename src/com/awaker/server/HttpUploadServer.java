package com.awaker.server;

import com.awaker.Awaker;
import com.awaker.data.TrackWrapper;
import com.awaker.server.json.UploadAnswer;
import com.awaker.util.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpUploadServer implements HttpHandler {

    private ServerListener listener;

    public static void start(ServerListener listener) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(Config.HTTP_UPLOAD_PORT), 0);
            server.createContext("/", new HttpUploadServer(listener));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HttpUploadServer(ServerListener listener) {
        this.listener = listener;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            //Header setzen
            //Access-Control-Header sind nur während debugging nötig
            httpExchange.getResponseHeaders().add("Content-type", "application/json");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "content-type, filename");
            httpExchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
            //Statuscode senden
            httpExchange.sendResponseHeaders(200, 0);

            UploadAnswer answer;
            System.out.println(httpExchange.getRequestMethod());
            if (httpExchange.getRequestMethod().toLowerCase().equals("options")) {
                answer = new UploadAnswer(null, "");
            } else {
                String fileName = httpExchange.getRequestHeaders().getFirst("filename");
                int length = Integer.parseInt(httpExchange.getRequestHeaders().getFirst("Content-Length"));

                //Datei runterladen und integrieren
                InputStream is = httpExchange.getRequestBody();
                TrackWrapper wrapper = listener.downloadFile(is, length, fileName, false);

                //antwort generieren
                answer = new UploadAnswer(wrapper, fileName);
            }

            //Antwort senden
            OutputStream os = httpExchange.getResponseBody();
            os.write(Awaker.GSON.toJson(answer).getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
