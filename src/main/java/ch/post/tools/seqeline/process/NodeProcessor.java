package ch.post.tools.seqeline.process;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import ch.post.tools.seqeline.catalog.Schema;
import ch.post.tools.seqeline.stack.*;
import lombok.RequiredArgsConstructor;
import org.joox.Match;

import java.util.List;
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

            case "VariableOrConstantDeclarator" -> {
                var variable = binding(node.child(0), BindingType.VARIABLE);
                context().declare(variable);
                stack.execute(new Assignment(variable), processSiblings(node.child(0)));
            }
            case "ProgramUnit" -> {
                var routine = new Binding(node.attr("MethodName").toLowerCase(), BindingType.ROUTINE);
                context().declare(routine);
                stack.execute(new LexicalScope(routine), processChildren(node));
            }

            case "CursorUnit" -> {
                var cursor = new Binding(name(node.child(0)), BindingType.CURSOR);
                context().declare(cursor);
                stack.execute(new LexicalScope(cursor), () -> {
                    process(node.child(1));
                    stack.execute(new Wrapper(new Binding("[cursor]", BindingType.RETURN)), ()->process(node.child(2)));
                });
            }

            case "FormalParameters" -> {
                var i = new AtomicInteger(0);
                node.children("FormalParameter").each().stream()
                        .map(param -> binding(param, BindingType.PARAMETER).position(i.getAndIncrement()))
                        .forEach(param -> context().declare(param));
            }

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
                node.children().each().forEach(argument -> {
                    if(argument.child("UnqualifiedID").isEmpty()) {
                        stack.execute(new Wrapper(new Binding("["+position+"]", BindingType.ARGUMENT).position(position.getAndIncrement())), processChildren(argument));
                    } else {
                        stack.execute(new Wrapper(binding(argument.child("UnqualifiedID"), BindingType.ARGUMENT)), processChildren(argument));
                    }
                });
            }

            case "ReturnStatement" ->
                stack.execute(new Wrapper(new Binding("[return]", BindingType.RETURN)), processChildren(node));

            case "Assignment", "OpenStatement" ->
                stack.execute(new Assignment(), processChildren(node));

            case "IfStatement", "CaseWhenClause" -> {
                // TODO: consider effects
                stack.execute(new IgnoreReturn(), () -> process(node.child(0)));
                processSiblings(node.child(0)).run();
            }

            case "ForStatement", "CursorForLoopStatement" -> stack.execute(new LexicalScope(), () -> {
                stack.execute(new Assignment(resolveNew(node.child(0), BindingType.STRUCTURE)),
                        () -> process(node.child(1)));
                node.child(1).nextAll().each().forEach(this::process);
            });

            case "SelectIntoStatement", "SelectStatement", "QueryBlock" -> {
                if (node.child("IntoClause").isNotEmpty()) {
                    stack.execute(new MultipleAssignment(intoVariables(node)),
                            () -> stack.execute(new SelectStatement(), processChildren(node)));
                } else {
                    stack.execute(new SelectStatement(), processChildren(node));
                }
            }

            case "WithClause" ->
                node.children("Name").each().forEach(child -> {
                    var name = binding(child, BindingType.STRUCTURE);
                    stack.execute(new Children(name), ()->process(child.next()));
                    context().declare(name);
                });

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

            case "TableName" -> {
                var struct = schema.resolve(name(node))
                        .map(relation -> stack.root().declare(relation))
                        .orElse(resolveNew(node, BindingType.STRUCTURE));
                if (!node.parent("TableReference").isEmpty()) {
                    context().returnBinding(struct);
                }
            }

            case "Column" -> {
                var column = resolveNew(node, BindingType.FIELD);
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

    private List<Binding> intoVariables(Match node) {
        var vars = node.child("IntoClause").children("VariableName").each();
        return vars.stream()
                .map(this::name)
                .map(QualifiedName::of)
                .map(t -> context().resolve(t).orElseThrow())
                .toList();
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
