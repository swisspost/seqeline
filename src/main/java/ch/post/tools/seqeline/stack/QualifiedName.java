package ch.post.tools.seqeline.stack;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualifiedName {
    private String prefix;
    private String name;

    public static QualifiedName of(String name) {
        return new QualifiedName(null, name);
    }

    public static QualifiedName of(String prefix, String name) {
        return new QualifiedName(prefix, name);
    }
}
