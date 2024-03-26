package ch.post.tools.seqeline.catalog;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class Schema {

    private Map<String, Relation> relations = new HashMap<>();

    @SneakyThrows
    public Schema(File schemaFile) {
        if(schemaFile.exists()) {
            JsonFactory factory = new JsonFactory();
            try (JsonParser parser = factory.createParser(schemaFile)) {
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if ("relations".equals(parser.getCurrentName())) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            String tableName = null;
                            String tableComment = null;
                            List<Column> columns = new ArrayList<>();
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
                                        columns.add(new Column(name, comment));
                                    }
                                }
                                if ("name".equals(parser.getCurrentName())) {
                                    tableName = parser.nextTextValue().toLowerCase();
                                }
                                if ("comment".equals(parser.getCurrentName())) {
                                    tableComment = parser.nextTextValue().toLowerCase();
                                }
                            }
                            Objects.requireNonNull(tableName);
                            Relation table = Relation.of(tableName, RelationType.TABLE, tableComment);
                            columns.forEach(column -> {
                                var binding = new Binding(column.name, BindingType.COLUMN);
                                binding.setComment(column.comment);
                                table.getBinding().addChild(binding);
                            });
                            relations.put(tableName, table);
                        }
                    }
                }
            }
        } else {
            log.warn("No metadata for the current schema. Columns will not be detected. Consider running seqeline -b.");
            Thread.sleep(1000);
        }
    }

    public Optional<Binding> resolve(String name) {
        return Optional.ofNullable(relations.get(name)).map(Relation::getBinding);
    }

    public Stream<Relation> relations() {
        return relations.values().stream();
    }

    private record Column(String name, String comment){}
}
