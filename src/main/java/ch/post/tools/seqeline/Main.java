package ch.post.tools.seqeline;

import lombok.SneakyThrows;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.joox.Match;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.eclipse.rdf4j.model.util.Values;
import java.io.File;
import java.io.IOException;

import static org.joox.JOOX.$;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        if (args.length < 1) {
            System.err.println("Missing filename argument");
            System.exit(1);
        }
        new Tree("app", args[0]).process("target/out.trig");
    }

    static class Tree {

        private static final String ROOT_NS = "https://lineage.post.ch/";
        private static final String model = "model";

        private String scopedNamespace;
        private Match root;
        private ModelBuilder modelBuilder;

        @SneakyThrows
        public Tree(String scope, String inputPath) {
            scopedNamespace = ROOT_NS + scope + "/";
            root = $(new File(inputPath));
            var lastSeparator = inputPath.lastIndexOf(File.separator);
            modelBuilder = new ModelBuilder()
                    .namedGraph(scopedNamespace + "graph/" + inputPath.substring(lastSeparator + 1))
                    .setNamespace(model, ROOT_NS + "model/");
        }

        public void process(String outputPath) {

        }

        private Element element(String id) {
            return this.root.document().getElementById(id);
        }
        private IRI nodeIri(Element element) {
            return switch (element.getTagName()) {
                case "package" -> Values.iri(model, element.getTagName());
                default -> Values.iri(model, element.getTagName()+"/"+element.getAttribute("id"));
            };
        }
    }

}
