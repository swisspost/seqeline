package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.BindingType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualifiedName {
    private String prefix;
    private String name;
    private boolean declared;
    private BindingType type = null;

    public static QualifiedName of(String name) {
        return new QualifiedName(null, name, false, null);
    }

    public static QualifiedName of(String prefix, String name) {
        return new QualifiedName(prefix, name, false, null);
    }

    public static QualifiedName of(String prefix, String name, boolean declared) {
        return new QualifiedName(prefix, name, declared, null);
    }


}
