package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Defines a binding as target for return bindings emitted in lower frames.
 */
@RequiredArgsConstructor
@AllArgsConstructor
public class MultipleAssignment extends Frame {

    private Deque<Binding> targets = new ArrayDeque<>();

    public MultipleAssignment(List<Binding> targets) {
        this.targets.addAll(targets);
    }

    @Override
    public void returnBinding(Binding binding) {
        binding.addOutput(targets.size() == 1 ? targets.getFirst() : targets.pop());
    }
}
