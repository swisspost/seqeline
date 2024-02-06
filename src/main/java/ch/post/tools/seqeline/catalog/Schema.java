package ch.post.tools.seqeline.catalog;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;

import java.io.File;
import java.util.*;

public class Schema {

    Map<String, Relation> relations = new HashMap<>();

    @SneakyThrows
    public Schema(String metadataFilePath) {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(new File(metadataFilePath))) {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if ("tables".equals(parser.getCurrentName())) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        String tableName = null;
                        List<String> columns = new ArrayList<>();
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            if ("columns".equals(parser.getCurrentName())) {
                                while (parser.nextToken() != JsonToken.END_ARRAY) {
                                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                                        if ("name".equals(parser.getCurrentName())) {
                                            columns.add(parser.nextTextValue().toLowerCase());
                                        }
                                    }
                                }
                            }
                            if ("name".equals(parser.getCurrentName())) {
                                tableName = parser.nextTextValue().toLowerCase();
                            }
                        }
                        Objects.requireNonNull(tableName);
                        Relation table = Relation.of(tableName, RelationType.TABLE);
                        columns.forEach(name -> table.getBinding().addChild(new Binding(name, BindingType.COLUMN)));
                        relations.put(tableName, table);
                    }
                }
            }
        }
    }

    public Optional<Binding> resolve(String name) {
        return Optional.ofNullable(relations.get(name)).map(Relation::getBinding);
    }
}
