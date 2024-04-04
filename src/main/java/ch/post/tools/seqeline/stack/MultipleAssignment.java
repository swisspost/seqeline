package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Defines a binding as target for return bindings emitted in lower frames.
 */
@RequiredArgsConstructor
@AllArgsConstructor
public class MultipleAssignment extends Frame {

    private Deque<Binding> targets = new ArrayDeque<>();

    private List<Binding> sources = new ArrayList<>();

    public MultipleAssignment(List<Binding> targets) {
        this.targets.addAll(targets);
    }

    @Override
    public void returnBinding(Binding binding) {
        this.sources.add(binding);
    }

    @Override
    protected void pop() {
        if (targets.size() == 1 && sources.size() > 1) {
            sources.forEach(source -> {
                var field = new Binding(source.getName(), BindingType.FIELD);
                source.addOutput(field);
                targets.element().addChild(field);
            });
        } else {
            sources.forEach(binding -> binding.addOutput(targets.size() == 1 ? targets.getFirst() : targets.pop()));
        }
    }
}
