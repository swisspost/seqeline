package ch.post.tools.seqeline;

import ch.post.tools.seqeline.metadata.Schema;
import ch.post.tools.seqeline.parser.Parser;
import ch.post.tools.seqeline.process.TreeProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class GenerationTest {

    private static final String sourceDir = "src/test/resources/generation";

    @SneakyThrows
    @ParameterizedTest
    @MethodSource
    public void testGeneration(String sourceFile) {
        var source = Files.readString(Path.of(sourceDir, sourceFile));
        var actual = execute(source).trim();
        var expected = Files.readString(Path.of(sourceDir, sourceFile.replaceAll("\\.sql$", ".ttl"))).trim();
        assertEquals(expected, actual);
    }

    public static Stream<String> testGeneration() {
        return Arrays.stream(new File(sourceDir).listFiles())
                .map(File::getName)
                .filter(name -> name.endsWith(".sql"));
    }

    private static Schema schema = new Schema();
    private Parser parser = new Parser();

    @BeforeAll
    public static void init() {
        schema.populate(new ByteArrayInputStream(schema().getBytes()));
    }

    @SneakyThrows
    private String execute(String source) {
        var root = parser.parse(new ByteArrayInputStream(source.getBytes()));
        var tree = new StringWriter();
        if(interactiveDev()) {
            root.write(tree);
        }
        var treeProcessor = new TreeProcessor("", "", "", root, schema);
        var model = treeProcessor.createModel();
        var out = new ByteArrayOutputStream();
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
        writer.startRDF();
        model.getNamespaces().stream()
                .filter(ns -> !ns.getPrefix().equals("rdf"))
                .forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
        for (var statement : model) {
            if(!statement.getPredicate().equals(RDF.TYPE)) {
                writer.handleStatement(statement);
            }
        }
        writer.endRDF();

        var rdfText = new String(out.toByteArray())
                .replaceAll("\\^\\^.*>", "")
                .replaceAll("(?m)^[ \t]*\r?\n", "");
        if(interactiveDev()) {
            System.out.println("https://www.ldf.fi/service/rdf-grapher?rdf=" + URLEncoder.encode(rdfText, StandardCharsets.UTF_8.toString()));
            System.out.println();
            System.out.println(tree);
            System.out.println();
            System.out.println(rdfText);
        }
        return rdfText;
    }

    private boolean interactiveDev() {
        // Test if we are in IntelliJ.
        return Optional.ofNullable(System.getProperties().get("java.class.path")).map(Objects::toString).map(p -> p.contains("idea_rt.jar")).orElse(false);
    }

    private static String schema() {
        return """
            {
                "relations": [
                    {
                        "name": "employee",
                        "type": "table",
                        "comment": "Our workforce",
                        "columns": [
                            {
                                "name": "firstname",
                                "type": "varchar2"
                            },
                            {
                                "name": "lastname",
                                "type": "varchar2"
                            },
                            {
                                "name": "dept_id",
                                "type": "number",
                                "comment": "Foreign key to departments"
                            }
                        ]
                    },
                    {
                        "name": "department",
                        "type": "view",
                        "columns": [
                            {
                                "name": "id",
                                "type": "number"
                            },
                            {
                                "name": "name",
                                "type": "varchar2"
                            }
                        ]
                    } 
                ]
            }
            """;
    }
}