package ch.post.tools.seqeline.metadata;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.Getter;

import java.util.List;

public record Relation(String name, RelationType type, String comment, List<Column> columns) {

    public Binding getBinding() {
        var result = new Binding(name, BindingType.RELATION);
        result.addType(type.toString());
        result.setComment(comment);
        columns.forEach(column -> {
            var binding = new Binding(column.name, BindingType.COLUMN);
            binding.setComment(column.comment);
            result.addChild(binding);
        });
        return result;
    }

    public record Column(String name, String comment){}
}
