package ch.post.tools.seqeline.metadata;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class Schema {

    private Map<String, Relation> relations = new HashMap<>();

    public Schema() {
    }

    @SneakyThrows
    public void populate(InputStream input) {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(input)) {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if ("relations".equals(parser.getCurrentName())) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        String relationName = null;
                        String relationComment = null;
                        String relationType= null;
                        List<Relation.Column> columns = new ArrayList<>();
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            if ("columns".equals(parser.getCurrentName())) {
                                while (parser.nextToken() != JsonToken.END_ARRAY) {
                                    String name = null;
                                    String comment = null;
                                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                                        if ("name".equals(parser.getCurrentName())) {
                                            name = parser.nextTextValue().toLowerCase();
                                        }
                                        if ("comment".equals(parser.getCurrentName())) {
                                            comment = parser.nextTextValue().toLowerCase();
                                        }
                                    }
                                    columns.add(new Relation.Column(name, comment));
                                }
                            }
                            if ("name".equals(parser.getCurrentName())) {
                                relationName = parser.nextTextValue().toLowerCase();
                            }
                            if ("type".equals(parser.getCurrentName())) {
                                relationType = parser.nextTextValue().toLowerCase();
                            }
                            if ("comment".equals(parser.getCurrentName())) {
                                relationComment = parser.nextTextValue().toLowerCase();
                            }
                        }
                        if("table".equals(relationType) || "view".equals(relationType)) {
                            Objects.requireNonNull(relationName);
                            Relation relation = new Relation(relationName, relationType, relationComment, columns);
                            relations.put(relationName, relation);
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows
    public Schema(File schemaFile) {
        if(schemaFile.exists()) {
            try(var in = new FileInputStream(schemaFile)) {
                populate(in);
            }
        } else {
            log.warn("No metadata for the current schema. Columns will not be detected. Consider running 'seqeline -b'.");
            Thread.sleep(1000);
        }
    }

    public Optional<Binding> resolve(String name) {
        return Optional.ofNullable(relations.get(name)).map(Relation::getBinding).map(binding->binding);
    }

    public Stream<Relation> relations() {
        return relations.values().stream();
    }
}
