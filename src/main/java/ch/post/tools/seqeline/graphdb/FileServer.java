package ch.post.tools.seqeline.graphdb;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Slf4j
public class FileServer {
    private final HttpServer server;
    private final ExecutorService executor;
    private final String basePath;
    private final Supplier<CountDownLatch> latchSupplier;

    @SneakyThrows
    public FileServer(int port, String basePath, Supplier<CountDownLatch> latchSupplier) {
        this.basePath = basePath;
        server = HttpServer.create(new java.net.InetSocketAddress(port), 0);
        server.createContext("/", new FileHandler());
        this.executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
        this.latchSupplier = latchSupplier;
    }

    public void stop() {
        server.stop(0);
        executor.shutdown();
    }

    class FileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            Path filePath = Paths.get(basePath + requestPath);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                log.info("Serving {}", filePath);
                byte[] fileBytes = Files.readAllBytes(filePath);
                exchange.sendResponseHeaders(200, fileBytes.length);
                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(fileBytes);
                responseBody.close();
            } else {
                log.error("Cannot find {}", filePath);
                String response = "File not found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(response.getBytes());
                responseBody.close();
            }
            latchSupplier.get().countDown();
        }
    }
}
