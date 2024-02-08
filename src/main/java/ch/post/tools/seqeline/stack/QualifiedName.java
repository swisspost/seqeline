package ch.post.tools.seqeline.stack;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualifiedName {
    private String prefix;
    private String name;
    private boolean functional;

    public static QualifiedName of(String name) {
        return new QualifiedName(null, name, true);
    }

    public static QualifiedName of(String prefix, String name) {
        return new QualifiedName(prefix, name, true);
    }

    public static QualifiedName of(String prefix, String name, boolean global) {
        return new QualifiedName(prefix, name, global);
    }
}
