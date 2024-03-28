package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.metadata.Schema;
import ch.post.tools.seqeline.stack.Stack;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.joox.Match;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.joox.JOOX.$;


@Log
public class TreeProcessor {

    private Match root;
    private ModelBuilder modelBuilder;
    private String line;
    private String line_data;
    private String localScope;

    private String graphName;

    private Schema schema;

    @SneakyThrows
    public TreeProcessor(String domain, String scope, String name, Match root, Schema schema) {
        this.schema = schema;
        this.root = root;

        domain = !domain.isBlank() ? "." + domain : "";
        scope = !scope.isBlank() ? "/" + scope : "";

        line = "https://schema"+domain+"/lineage/";
        line_data = "https://data"+domain + scope + "/lineage/";
        localScope = name;
        graphName = "https://graph" + domain + scope + "/lineage/" + localScope;
        modelBuilder = new ModelBuilder()
                .namedGraph(graphName)
                .setNamespace("rdf", RDF.NAMESPACE)
                .setNamespace("rdfs", RDFS.NAMESPACE)
                .setNamespace("line", line)
                .setNamespace("line_data", line_data);
    }

    @SneakyThrows
    public String process(OutputStream out) {

        var model = createModel();

        RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, out);
        writer.startRDF();
        model.getNamespaces().forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
        for (var statement : model) {
            writer.handleStatement(statement);
        }
        writer.endRDF();
        return graphName;
    }

    public Model createModel() {
        Stack stack = new Stack();
        Binding.resetCounter();
        new NodeProcessor(stack, schema).process(root);

        Map<Binding, IRI> createdNodes = new HashMap<>();

        stack.root().getBindings().stream().forEach(primary -> {
            var primaryName = primary.getName();
            primary.setGlobalName(primaryName);
            primary.children().forEach(secondary ->
                    secondary.setGlobalName(primaryName + "." + secondary.getName()));
        });

        stack.root().getBindings().stream().forEach(binding ->
                createNode(modelBuilder, binding, createdNodes));

        return modelBuilder.build();
    }

    private IRI createNode(ModelBuilder modelBuilder, Binding binding, Map<Binding, IRI> createdNodes) {
        var existing = createdNodes.get(binding);
        if(existing != null) {
            return existing;
        }
        var name = Optional.ofNullable(binding.getGlobalName())
                .orElse(localScope+":" +binding.getType().toString().toLowerCase()+":"+ binding.getId());
        var nodeIri = iri(line_data, name);
        createdNodes.put(binding, nodeIri);

        modelBuilder.add(nodeIri, RDF.TYPE, iri(line, capitalize(binding.getType().toString())));
        binding.additionalTypes().forEach(type ->
                modelBuilder.add(nodeIri, RDF.TYPE, capitalize(type)));
        modelBuilder.add(nodeIri, RDFS.LABEL, literal(binding.getName().toLowerCase()));
        Optional.ofNullable(binding.getComment()).ifPresent(comment ->
                modelBuilder.add(nodeIri, RDFS.COMMENT, literal(comment)));
        Optional.ofNullable(binding.getPosition()).ifPresent(position ->
                modelBuilder.add(nodeIri, iri(line, "position"), literal(position)));
        binding.children().forEach(child ->
                modelBuilder.add(nodeIri, iri(line, "member"), createNode(modelBuilder, child, createdNodes)));
        binding.outputs().forEach(output ->
                modelBuilder.add(nodeIri, iri(line, "output"), createNode(modelBuilder, output, createdNodes)));
        binding.effects().forEach(effect ->
                modelBuilder.add(nodeIri, iri(line, "effect"), createNode(modelBuilder, effect, createdNodes)));
        binding.references().forEach(reference ->
                modelBuilder.add(nodeIri, iri(line, "reference"), createNode(modelBuilder, reference, createdNodes)));
        return nodeIri;
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
