package ch.post.tools.seqeline;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.joox.Context;
import org.joox.Match;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.joox.JOOX.$;

@Log
public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        if (args.length < 1) {
            log.severe("Missing filename argument");
            System.exit(1);
        }
        new Tree("app", args[0]).process("target/out.trig");
    }

    static class Tree {
        private static final String line = "https://schema.post.ch/lineage/";
        private static final String DATA_ROOT_NS = "https://data.post.ch/";

        private Match root;
        private ModelBuilder modelBuilder;
        private String line_data;
        private String localScope;
        private Map<String,Element> nodes = new HashMap<>();

        @SneakyThrows
        public Tree(String scope, String inputPath) {
            root = $(new File(inputPath));
            var lastSeparator = inputPath.lastIndexOf(File.separator);
            line_data = DATA_ROOT_NS + scope + "/lineage/";
            localScope = inputPath.substring(lastSeparator + 1);
            modelBuilder = new ModelBuilder()
                    .namedGraph("https://graph.post.ch/" + scope + "/lineage/" + localScope)
                    .setNamespace("rdf", RDF.NAMESPACE)
                    .setNamespace("rdfs", RDFS.NAMESPACE)
                    .setNamespace("line", line)
                    .setNamespace("line_data", line_data);
        }

        @SneakyThrows
        public void process(String outputPath) {

            Model model = modelBuilder.build();
            var out = new FileOutputStream("target/graph.trig");
            RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, out);
            writer.startRDF();
            model.getNamespaces().forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
            for (var statement : model) {
                writer.handleStatement(statement);
            }
            writer.endRDF();

        }
    }
}
