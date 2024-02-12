package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.catalog.Schema;
import ch.post.tools.seqeline.stack.Stack;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    public TreeProcessor(String domain, String scope, String inputPath, Schema schema) {
        this.schema = schema;
        root = $(new File(inputPath));
        var lastSeparator = inputPath.lastIndexOf(File.separator);
        line = "https://schema."+domain+"/lineage/";
        line_data = "https://data."+domain+"/" + scope + "/lineage/";
        localScope = inputPath.substring(lastSeparator + 1).replace(".xml", "");
        graphName = "https://graph." + domain + "/" + scope + "/lineage/" + localScope;
        modelBuilder = new ModelBuilder()
                .namedGraph(graphName)
                .setNamespace("rdf", RDF.NAMESPACE)
                .setNamespace("rdfs", RDFS.NAMESPACE)
                .setNamespace("line", line)
                .setNamespace("line_data", line_data);
    }

    @SneakyThrows
    public void process(String outputPath) {

        Stack stack = new Stack();
        new NodeProcessor(stack, schema).process(root);

        Map<Binding, IRI> createdNodes = new HashMap<>();
        stack.root().getBindings().stream().forEach(binding ->
                createNode(modelBuilder, binding, 2, null, createdNodes));

        Model model = modelBuilder.build();
        var out = new FileOutputStream(outputPath);
        RDFWriter writer = Rio.createWriter(RDFFormat.TRIG, out);
        writer.startRDF();
        model.getNamespaces().forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));
        for (var statement : model) {
            writer.handleStatement(statement);
        }
        writer.endRDF();
        out.close();

        URL url = new URL("http://localhost:7200/rest/repositories/lineage/import/upload/url");
        String sourceUrl = "http://localhost:8000/out.trig";
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        var mapper = new ObjectMapper();
        var request = mapper.createObjectNode();
        var graphs = mapper.createArrayNode();
        var fileNames = mapper.createArrayNode();
        fileNames.add(new File(outputPath).getAbsolutePath());
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

    private IRI createNode(ModelBuilder modelBuilder, Binding binding, int nameDepth, String prefix, Map<Binding, IRI> createdNodes) {
        var existing = createdNodes.get(binding);
        if(existing != null) {
            return existing;
        }
        if (prefix != null) {
            prefix = prefix + ".";
        } else {
            prefix = "";
        }
        var name = nameDepth > 0 ? prefix + binding.getName().toLowerCase() : "binding:" + binding.getId();
        var nodeIri = iri(line_data, name);
        createdNodes.put(binding, nodeIri);

        modelBuilder.add(nodeIri, RDF.TYPE, iri(line, capitalize(binding.getType().toString())));
        modelBuilder.add(nodeIri, RDFS.LABEL, literal(binding.getName().toLowerCase()));
        binding.children().forEach(child ->
                modelBuilder.add(nodeIri, iri(line, "member"), createNode(modelBuilder, child, nameDepth - 1, name, createdNodes)));
        binding.outputs().forEach(output ->
                modelBuilder.add(nodeIri, iri(line, "output"), createNode(modelBuilder, output, 0, null, createdNodes)));
        binding.effects().forEach(effect ->
                modelBuilder.add(nodeIri, iri(line, "effect"), createNode(modelBuilder, effect, 0, null, createdNodes)));
        return nodeIri;
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
