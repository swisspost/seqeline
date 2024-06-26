package ch.post.tools.seqeline;

import ch.post.tools.seqeline.metadata.MetadataFetcher;
import ch.post.tools.seqeline.metadata.Schema;
import ch.post.tools.seqeline.graphdb.GraphDbPublisher;
import ch.post.tools.seqeline.parser.ParseException;
import ch.post.tools.seqeline.parser.Parser;
import ch.post.tools.seqeline.process.TreeProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joox.Match;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.joox.JOOX.$;


@Command(name = "seqeline", description = "Generate RDF data lineage graph from PL/SQL code.")
@Slf4j
public class Main implements Callable<Integer> {

    static class GenerationArgs {
        @Parameters(description = "Source files or directories", arity = "1..*")
        private List<File> paths;
    }

    static class DatabaseArgs {
        @Option(names = {"-b", "--database"}, required = true, description = "JDBC URL to fetch metadata (if present, seqeline only fetches the metadata)")
        private String dbUrl;

        @Option(names = {"-u", "--username"}, required = true, description = "Database user")
        private String username;

        @Option(names = {"-p", "--password"}, required = true, description = "Database password or @<file> containing password.")
        private String password;
    }

    static class Args {
        @ArgGroup(exclusive = false, heading = "File system Sources %n")
        GenerationArgs generation;

        @ArgGroup(exclusive = false, heading = "Fetch metadata from database %n")
        DatabaseArgs database;
    }

    @ArgGroup(exclusive = true, multiplicity = "1")
    Args args;

    @Option(names = {"-o", "--output-dir"}, description = "Output directory for graphs", defaultValue = "target/seqeline/graph")
    private File outputDir;

    @Option(names = {"--tree-only"}, description = "Only generate tree")
    private boolean treeOnly;

    @Option(names = {"-t", "--force-tree"}, description = "Ignore cached files and force tree generation")
    private boolean forceTree;

    @Option(names = {"-g", "--force-graph"}, description = "Ignore cached files and force graph generation")
    private boolean forceGraph;

    @Option(names = {"--publish"}, description = "Publish graphs to GraphDB")
    private boolean publish;

    @Option(names = {"--graphdb-url"}, description = "GraphDB repository to publish to.", defaultValue =
            "http://localhost:7200/rest/repositories/lineage/")
    private String graphDbRepositoryUrl;

    @Option(names = {"-i", "--ignore-errors"}, description = "Continue on errors")
    private boolean continueOnError;

    @Option(names = {"-d", "--domain"}, description = "Domain name to use in RDF URLs", defaultValue = "")
    private String domain;

    @Option(names = {"-a", "--application"}, description = "Application name", defaultValue = "")
    private String application;

    @Option(names = {"-c", "--cache-dir"}, description = "Cache directory", defaultValue = "target/seqeline")
    private File cacheDir;

    private static final String extension = "\\.[^\\.]+$";

    @Override
    public Integer call() throws Exception {
        cacheDir.mkdirs();
        var metadataDir = new File(new File(cacheDir, "metadata"), application);
        var schemaFile = new File(metadataDir, "schema.json");

        if (args.database != null) {
            // Fetch metadata
            metadataDir.mkdirs();
            String password;
            if (args.database.password.startsWith("@")) {
                password = Files.readString(Path.of(args.database.password.substring(1)));
            } else {
                password = args.database.password;
            }
            var fetcher = new MetadataFetcher(args.database.dbUrl, args.database.username, password);

            try (var out = new FileOutputStream(schemaFile)) {
                fetcher.fetchMetadata(
                        getClass().getClassLoader().getResourceAsStream("metadata.sql"),
                        out
                );
            }
        }

        Schema schema = new Schema(schemaFile);

        if(args.database !=null) {
            var relationCount = schema.relations().count();
            var columnCount = schema.relations().flatMap(r -> r.getBinding().children()).count();
            log.info("Fetched "+columnCount+" columns from "+relationCount+" relations.");

            return 0;
        }

        var treeDir = new File(new File(cacheDir, "tree"), application);
        outputDir = new File(outputDir, application);

        treeDir.mkdirs();
        outputDir.mkdirs();

        var files = args.generation.paths.stream().flatMap(path -> {
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

        GraphDbPublisher graphDbPublisher = new GraphDbPublisher(graphDbRepositoryUrl, outputDir.getAbsolutePath());

        if(publish) {
            graphDbPublisher.start();
        }

        try {
            for (var sourceFile : files) {
                var treeFile = new File(treeDir, sourceFile.getName().replaceAll(extension, ".xml"));
                var tree = makeTree(sourceFile, treeFile);
                var graphFile = new File(outputDir, sourceFile.getName().replaceAll(extension, ".trig"));
                if(tree.isPresent()) {
                    if (forceGraph || shouldGenerate(treeFile, graphFile) && !treeOnly) {
                        log.info("Generating graph ...");
                        String graphName;
                        try (var out = new FileOutputStream(graphFile)) {
                            graphName = new TreeProcessor(domain, application, sourceFile.getName().replaceAll(extension, ""), tree.get(), schema).process(out);
                        }
                        if (publish) {
                            graphDbPublisher.publishToGraphDb(graphName, graphFile.getName()).await();
                        }
                    } else {
                        log.info("Graph already up-to-date.");
                    }
                } else {
                    log.info("Skipped.");
                }
            }
            log.info("done.");
            return 0;
        } catch (ParseException e) {
            log.error("Finished with errors.");
            return 1;
        } finally {
            graphDbPublisher.close();
        }
    }

    @SneakyThrows
    private Optional<Match> makeTree(File source, File target) {
        Match result;
        log.info("Processing " + source + " ...");
        try {
            if (forceTree || shouldGenerate(source, target)) {
                try (var out = new FileOutputStream(target)) {
                    try (var in = new FileInputStream(source)) {
                        result = new Parser().parse(in);
                    }
                    result.write(out);
                }
            } else {
                log.info("Using cached tree.");
                result = $(target);
            }
            return Optional.of(result);
        } catch (ParseException e) {
            if(target.delete()) {
                log.info("Deleted " + target);
            } else {
                log.error("Deleted " + target);
            }

            if (!e.isSkipped() && !continueOnError) {
                throw e;
            }
        }
        return Optional.empty();
    }

    private boolean shouldGenerate(File source, File target) {
        return !target.exists() || target.length() == 0 || source.lastModified() > target.lastModified();
    }

    public static void main(String... args) throws IOException, SAXException {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
