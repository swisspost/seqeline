package ch.post.tools.seqeline.graphdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class GraphDbPublisher {

    private final String repositoryUrl;
    private final static int SERVER_PORT = 25869;
    private FileServer fileServer;
    private String basePath;
    private boolean started = false;
    private CountDownLatch latch;

    public GraphDbPublisher(String repositoryUrl, String basePath) {
        this.basePath = basePath;
        this.repositoryUrl = repositoryUrl;
    }

    @SneakyThrows
    public CountDownLatch publishToGraphDb(String graphName, String graphFile) {
        latch = new CountDownLatch(1);
        URL url = new URL(repositoryUrl + "/import/upload/url");
        String sourceUrl = "http://localhost:"+ SERVER_PORT +"/"+graphFile;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        var mapper = new ObjectMapper();
        var request = mapper.createObjectNode();
        var graphs = mapper.createArrayNode();
        var fileNames = mapper.createArrayNode();
        fileNames.add(graphFile);
        request.set("data", new TextNode(sourceUrl));
        request.set("name", new TextNode(sourceUrl));
        graphs.add(graphName);
        request.set("replaceGraphs", graphs);

        try (OutputStream os = con.getOutputStream()) {
            mapper.writeValue(os, request);
        }

        if (con.getResponseCode() != 200) {
            InputStream err = con.getErrorStream();
            if (err != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(err, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    log.error(response.toString());
                }
            }
        }
        return latch;
    }

    public void start() {
        if(!started) {
            fileServer = new FileServer(SERVER_PORT, basePath, () -> latch);
            started = true;
        }
    }

    public void close() {
        if(started) {
            fileServer.stop();
        }
    }
}
