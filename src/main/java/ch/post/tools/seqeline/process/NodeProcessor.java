package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import ch.post.tools.seqeline.stack.LexicalScope;
import ch.post.tools.seqeline.stack.Stack;
import lombok.RequiredArgsConstructor;
import org.joox.Match;

@RequiredArgsConstructor
public class NodeProcessor {

    private final Stack stack;

    public void process(Match node) {
        switch (node.tag()) {
            case "PackageBody" -> {
                var pack = new Binding(name(node), BindingType.PACKAGE);
                stack.top().declare(pack);
                stack.execute(LexicalScope.builder().owner(pack).build(),
                        () -> processChildren(node));
            }
            case "MethodDeclarator" -> {
                var routine = new Binding(name(node), BindingType.ROUTINE);
                stack.top().declare(routine);
                stack.execute(new LexicalScope(),
                        () -> processChildren(node));
            }
            default ->
                processChildren(node);
        }
    }

    private String name(Match node) {
        return node.attr("CanonicalImage");
    }

    private void processChildren(Match node) {
        node.children().each().forEach(this::process);
    }
}
