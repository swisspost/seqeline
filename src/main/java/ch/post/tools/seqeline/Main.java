package ch.post.tools.seqeline;

import ch.post.tools.seqeline.catalog.Schema;
import ch.post.tools.seqeline.parser.Parser;
import ch.post.tools.seqeline.process.TreeProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joox.Match;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.joox.JOOX.$;


@Command(name = "sequeline", description = "Generate RDF data lineage graph from PL/SQL code.")
@Slf4j
public class Main implements Callable<Integer> {

    @Parameters(description = "Source files or directories", arity = "1..*")
    private List<File> paths;

    @Option(names = {"-d", "--domain"}, description = "Cache directory for parse trees", defaultValue = "")
    private String domain;

    @Option(names = {"-a", "--application"}, description = "Application name", defaultValue = "app")
    private String application;

    @Option(names = {"-c", "--cache-dir"}, description = "Cache directory for parse trees", defaultValue = "target/sequeline/tree")
    private File cacheDir;

    @Option(names = {"-o", "--output-dir"}, description = "Output directory for graphs", defaultValue = "target/sequeline/graph")
    private File outputDir;

    @Option(names = {"-t", "--tree-only"}, description = "Only generate tree")
    private boolean treeOnly;

    @Option(names = {"-f", "--force"}, description = "Ignore cached files and force generation")
    private boolean force;

    @Option(names = {"-p", "--publish"}, description = "Publish to localhost Graph DB")
    private boolean publish;

    private static final String extension = "\\.[^\\.]+$";

    @Override
    public Integer call() throws Exception {
        Schema schema = new Schema("data/model/metadata.json");
        cacheDir.mkdirs();
        outputDir.mkdirs();

        var files = paths.stream().flatMap(path -> {
            if (path.isFile()) {
                return Stream.of(path.getAbsoluteFile());
            } else {
                try (Stream<Path> stream = Files.walk(Paths.get(path.getAbsolutePath()))) {
                    return stream.filter(file -> !Files.isDirectory(file))
                            .map(Path::toFile)
                            .toList()
                            .stream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).toList();


        for (var sourceFile : files) {
            var treeFile = new File(cacheDir, sourceFile.getName().replaceAll(extension, ".xml"));
            var tree = makeTree(sourceFile, treeFile);
            var graphFile = new File(outputDir, sourceFile.getName().replaceAll(extension, ".trig"));
            if(force || shouldGenerate(treeFile, graphFile) && !treeOnly) {
                log.info("Generating graph ...");
                String graphName;
                try (var out = new FileOutputStream(graphFile)) {
                    graphName = new TreeProcessor(domain, application, sourceFile.getName().replaceAll(extension,""), tree, schema).process(out);
                }
                if(publish) {
                    publishToGraphDb(graphName, graphFile);
                }
            } else {
                log.info("Graph already up-to-date.");
            }
        }
        log.info("done.");
        return 0;
    }

    @SneakyThrows
    private Match makeTree(File source, File target) {
        Match result;
        log.info("Processing "+source+" ...");
        if(force || shouldGenerate(source, target)) {
            try(var out = new FileOutputStream(target)) {
                try(var in = new FileInputStream(source)) {
                    result = new Parser().parse(in);
                }
                result.write(out);
            }
        } else {
            log.info("Using cached tree.");
            result = $(target);
        }
        return result;
    }

    private boolean shouldGenerate(File source, File target) {
        return !target.exists() || source.lastModified() > target.lastModified();
    }

    @SneakyThrows
    private void publishToGraphDb(String graphName, File graphFile) {
        URL url = new URL("http://localhost:7200/rest/repositories/lineage/import/upload/url");
        String sourceUrl = "http://localhost:8000/"+graphFile;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        var mapper = new ObjectMapper();
        var request = mapper.createObjectNode();
        var graphs = mapper.createArrayNode();
        var fileNames = mapper.createArrayNode();
        fileNames.add(graphFile.getAbsolutePath());
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
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    log.info(response.toString());
                }
            }
        }
    }

    public static void main(String... args) throws IOException, SAXException {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
