package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.stack.Stack;
import lombok.SneakyThrows;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.joox.Match;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.joox.JOOX.$;

public class TreeProcessor {

    private Match root;
    private ModelBuilder modelBuilder;
    private String line;
    private String line_data;
    private String localScope;

    @SneakyThrows
    public TreeProcessor(String domain, String scope, String inputPath) {
        root = $(new File(inputPath));
        var lastSeparator = inputPath.lastIndexOf(File.separator);
        line = "https://schema."+domain+"/lineage/";
        line_data = "https://data."+domain+"/" + scope + "/lineage/";
        localScope = inputPath.substring(lastSeparator + 1).replace(".xml", "");
        modelBuilder = new ModelBuilder()
                .namedGraph("https://graph."+domain +"/"+ scope + "/lineage/" + localScope)
                .setNamespace("rdf", RDF.NAMESPACE)
                .setNamespace("rdfs", RDFS.NAMESPACE)
                .setNamespace("line", line)
                .setNamespace("line_data", line_data);
    }

    @SneakyThrows
    public void process(String outputPath) {

        Stack stack = new Stack();
        new NodeProcessor(stack).process(root);

        AtomicInteger nodeId = new AtomicInteger(0);
        stack.root().getBindings().stream().forEach(binding ->
                createNode(modelBuilder, nodeId, binding, true));

        Model model = modelBuilder.build();
        var out = new FileOutputStream(outputPath);
        RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, out);
        writer.startRDF();
        model.getNamespaces().forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
        for (var statement : model) {
            writer.handleStatement(statement);
        }
        writer.endRDF();
    }

    private IRI createNode(ModelBuilder modelBuilder, AtomicInteger id, Binding binding, boolean named) {
        var name = named ? binding.getName().toLowerCase() : String.valueOf(id.getAndIncrement());
        var nodeIri = iri(line_data, name);
        modelBuilder.add(nodeIri, RDF.TYPE, iri(line, capitalize(binding.getType().toString())));
        binding.children().forEach(binding1 -> {
            var childIri = iri(line_data, name + "." + binding.getName().toLowerCase());
            modelBuilder.add(nodeIri, iri(line, "member"), childIri);
            modelBuilder.add(nodeIri, RDF.TYPE, iri(line, capitalize(binding.getType().toString())));
        });
        binding.outputs().forEach(output ->
                modelBuilder.add(nodeIri, iri(line, "output"), createNode(modelBuilder, id, output, false)));
        return nodeIri;
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
