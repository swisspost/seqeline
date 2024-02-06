package ch.post.tools.seqeline.catalog;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class Relation {

    private Relation() {};

    public static Relation of(String name, RelationType type) {
        var result = new Relation();
        result.binding = new Binding(name, BindingType.RELATION);
        result.type = type;
        return result;
    }

    private Binding binding;
    private RelationType type;
}
