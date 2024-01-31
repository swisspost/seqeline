package ch.post.tools.seqeline.stack;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Reference {
    private String prefix;
    private String name;
}
