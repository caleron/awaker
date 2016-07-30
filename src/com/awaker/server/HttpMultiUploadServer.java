package com.awaker.server;

import com.awaker.Awaker;
import com.awaker.server.json.UploadAnswer;
import com.awaker.util.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class HttpMultiUploadServer implements HttpHandler {

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(Config.HTTP_UPLOAD_PORT), 0);
            server.createContext("/", new HttpMultiUploadServer());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
        ServletFileUpload fileUpload = new ServletFileUpload(diskFileItemFactory);

        try {
            List<FileItem> result = fileUpload.parseRequest(new HttpHandlerRequestContext(httpExchange));

            httpExchange.getResponseHeaders().add("Content-type", "application/json");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "content-type");
            httpExchange.sendResponseHeaders(200, 0);

            OutputStream os = httpExchange.getResponseBody();
            UploadAnswer answer = new UploadAnswer(null, "");
            for (FileItem fi : result) {
                answer.status += fi.getName() + "\r\n";
                System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName());
            }
            os.write(Awaker.GSON.toJson(answer).getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static class HttpHandlerRequestContext implements RequestContext {

        private HttpExchange http;

        HttpHandlerRequestContext(HttpExchange http) {
            this.http = http;
        }

        @Override
        public String getCharacterEncoding() {
            //Need to figure this out yet
            return "UTF-8";
        }

        @Override
        public String getContentType() {
            //Content-Type includes the boundary needed for parsing
            return http.getRequestHeaders().getFirst("Content-type");
        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            //pass on input stream
            return http.getRequestBody();
        }
    }
}
