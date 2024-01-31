package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.stack.Stack;
import lombok.RequiredArgsConstructor;
import org.joox.Match;

@RequiredArgsConstructor
public class NodeProcessor {

    private final Stack stack;

    public void process(Match node) {
        switch (node.tag()) {
            case "PackageBody":
                processChildren(node);
                break;
            default:
                processChildren(node);
        }
    }

    private void processChildren(Match node) {
        node.children().each().forEach(this::process);
    }
}
