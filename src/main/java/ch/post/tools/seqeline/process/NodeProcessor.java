package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import ch.post.tools.seqeline.catalog.Schema;
import ch.post.tools.seqeline.stack.*;
import lombok.RequiredArgsConstructor;
import org.joox.Match;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class NodeProcessor {

    private final Stack stack;
    private final Schema schema;

    public void process(Match node) {
        if(node.isEmpty()) {
            return;
        }

        switch (node.tag()) {
            case "PackageSpecification" -> skip();

            case "PackageBody" -> {
                var pack = new Binding(name(node), BindingType.PACKAGE);
                context().declare(pack);
                stack.execute(new LexicalScope(pack), processChildren(node));
            }

            case "VariableOrConstantDeclaratorId" ->
                context().declare(binding(node, BindingType.VARIABLE));

            case "ProgramUnit" -> {
                var routine = new Binding(node.attr("MethodName"), BindingType.ROUTINE);
                context().declare(routine);
                stack.execute(new LexicalScope(routine), processChildren(node));
            }

            case "FormalParameter" -> context().declare(binding(node, BindingType.PARAMETER));

            case "FunctionCall" -> stack.execute(new RoutineCall(), processChildren(node));

            case "FunctionName" -> {
                var qualifiedName = QualifiedName.builder().type(BindingType.ROUTINE);
                if(node.children().each().size() == 2) {
                    qualifiedName.prefix(name(node.child(0))).name(name(node.child(1)));
                } else {
                    qualifiedName.name(name(node.child(0)));
                }
                context().returnBinding(context().resolve(qualifiedName.build()).orElseThrow());
            }

            case "ArgumentList" -> {
                AtomicInteger position = new AtomicInteger(0);
                // TODO: manage support named parameters
                node.children().each().forEach(argument ->
                        stack.execute(new Wrapper(new Binding("["+position.getAndIncrement()+"]", BindingType.ARGUMENT)), processChildren(argument)));
            }

            case "ReturnStatement" ->
                stack.execute(new Wrapper(new Binding("[return]", BindingType.RETURN)), processChildren(node));

            case "Assignment" ->
                stack.execute(new Assignment(resolveNew(node.child(0), BindingType.VARIABLE)),
                        processSiblings(node.child(0)));

            case "IfStatement" -> {
                // TODO: consider effects
                stack.execute(new IgnoreReturn(), () -> process(node.child(0)));
                processSiblings(node.child(0)).run();
            }

            case "OpenStatement" -> stack.execute(new Assignment(resolveNew(node.child(0), BindingType.CURSOR)),
                    processSiblings(node.child(0)));

            case "CursorForLoopStatement" -> stack.execute(new LexicalScope(), () ->
                    stack.execute(new Assignment(resolveNew(node.child(0), BindingType.RECORD)),
                    processSiblings(node.child(0))));

            case "SelectStatement", "QueryBlock" -> stack.execute(new SelectStatement(), processChildren(node));

            case "SelectIntoStatement" ->
                    stack.execute(new Assignment(context().resolve(
                            QualifiedName.of(name(node.child("IntoClause").child("VariableName")))).orElseThrow()),
                            () -> stack.execute(new SelectStatement(), processChildren(node)));

            case "SelectList" ->
                stack.execute(new SelectStatement.SelectList(), () ->
                        node.children().each().forEach(child -> {
                            if (child.is("ColumnAlias")) {
                                Binding alias = binding(child, BindingType.ALIAS);
                                context().declare(alias);
                                context().returnBinding(alias);
                                stack.execute(new Assignment(alias), () -> process(child.prev()));
                            } else {
                                process(child.prev());
                                if (child.next().isEmpty()) {
                                    process(child);
                                }
                            }
                        }));

            case "TableName" -> context().returnBinding(
                    schema.resolve(name(node))
                            .map(relation -> stack.root().declare(relation))
                            .orElse(resolveNew(node, BindingType.RECORD)));

            case "Column" -> {
                var column = context().declare(binding(node, BindingType.FIELD));
                context().returnBinding(column);
                node.prev("TableName").each().forEach(relation -> context().resolve(QualifiedName.of(name(relation))).ifPresent(r -> r.addChild(column)));
            }

            case "JoinClause", "WhereClause" -> {
                stack.execute(new SelectStatement.EffectClause(), processChildren(node));
            }

            case "SubqueryOperation" -> {
                if(node.parent("SelectStatement").isNotEmpty() && node.prevAll("SubqueryOperation").isEmpty()) {
                    stack.pop();
                }
            }

            default -> processChildren(node).run();
        }
    }

    private Binding resolveNew(Match node, BindingType type) {
        return context().resolve(QualifiedName.of(null, name(node), true)).or(() ->
                Optional.of(context().declare(binding(node, type)))).orElseThrow();
    }

    private Binding binding(Match node, BindingType type) {
        return new Binding(name(node), type);
    }

    private String name(Match node) {
        return node.attr("CanonicalImage").toLowerCase();
    }

    private Runnable processChildren(Match node) {
        return () -> node.children().each().forEach(this::process);
    }

    private Runnable processSiblings(Match node) {
        return () -> node.siblings().each().forEach(this::process);
    }

    private void skip() {
        // do nothing
    }

    private Frame context() {
        return stack.top();
    }
}
