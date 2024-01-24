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
            final var nodeCount = new AtomicInteger();
            root.find(context ->
                            !context.element().getTagName().equals("relationship") &&
                            !$(context).parent().tag().equals("relationship"))
                    .map(Context::element)
                    .stream()
                    .map(this::indexNode)
                    .map(this::createLabel)
                    .map(this::createType)
                    .map(this::createProperties)
                    .map(this::linkToParent)
                    .forEach( node -> nodeCount.incrementAndGet());
            log.info(nodeCount + " nodes processed");

            var relationCount = root.find("relationship")
                    .each(relationship -> $(relationship).children("target")
                            .each(target -> $(relationship).child("source")
                                    .each(source -> {
                                        var effect = $(relationship).attr("effectType");
                                        if(effect == null) {
                                            effect = "effect";
                                        }
                                        var propertyIri = iri(line, effect);
                                        var sourceElement = element(source.element().getAttribute("id"));
                                        if(sourceElement == null) {
                                            log.warning("Source node not found: "+source.element().getAttribute("id"));
                                            return;
                                        }
                                        var targetElement = element(target.element().getAttribute("id"));
                                        if(targetElement == null) {
                                            log.warning("Target node not found: "+target.element().getAttribute("id"));
                                            return;
                                        }
                                        modelBuilder.add(nodeIri(sourceElement), propertyIri, nodeIri(targetElement));
                                        modelBuilder.add(propertyIri, RDF.TYPE, iri(line, $(relationship).attr("type")));
                                        modelBuilder.add(propertyIri, RDF.TYPE, iri(line, "dataflow"));
                                    })
                            ))
                    .size();
            log.info(relationCount + " relations processed");
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

        private Element indexNode(Element element) {
            nodes.put(element.getAttribute("id"), element);
            return element;
        }

        private Element createLabel(Element element) {
            var label = name(element);
            if (label.isBlank()) {
                label = element.getTagName();
            }
            modelBuilder.add(nodeIri(element), RDFS.LABEL, literal(label));
            return element;
        }

        private Element createProperties(Element element) {
            var attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                modelBuilder.add(nodeIri(element), iri(line, attributes.item(i).getLocalName()), literal(attributes.item(i).getNodeValue()));
            }
            return element;
        }

        private Element createType(Element element) {
            var type = iri(line, element.getTagName().substring(0, 1).toUpperCase() + element.getTagName().substring(1));
            modelBuilder.add(nodeIri(element), RDF.TYPE, type);
            modelBuilder.add(type, RDF.TYPE, RDFS.CLASS);
            return element;
        }

        private Element linkToParent(Element element) {
            modelBuilder.add(nodeIri(element), iri(line, "parent"), nodeIri($(element).parent().get(0)));
            return element;
        }

        private Element element(String id) {
            return nodes.get(id);
        }

        private String name(Element element) {
            return element.getAttribute("name");
        }

        private IRI nodeIri(Element element) {
            return switch (element.getTagName()) {
                case "package", "sequence", "table" -> nodeNameIri(element);
                case "procedure", "function" -> $(element).parent().filter("package").isNotEmpty() ?
                        nodeNameIri($(element).parent().get(0), element) :
                        $(element).parent().filter("dlineage").isNotEmpty() ?
                                nodeNameIri(element) :
                                nodeIdIri(element);
                case "column" -> $(element).parent().filter("table, view").isNotEmpty() ?
                        nodeNameIri($(element).parent().get(0), element) :
                        nodeIdIri(element);
                default -> nodeIdIri(element);
            };
        }

        private String localName(Element element) {
            return element.getTagName() + ":" + name(element);
        }

        private IRI nodeNameIri(Element element) {
            return iri(line_data, localName(element));
        }

        private IRI nodeNameIri(Element parent, Element element) {
            return iri(line_data, localName(parent) + ":" + localName(element));
        }

        private IRI nodeIdIri(Element element) {
            return iri(line_data, localScope + ":" + element.getAttribute("id"));
        }
    }
}
