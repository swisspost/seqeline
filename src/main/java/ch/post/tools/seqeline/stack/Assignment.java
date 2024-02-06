package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import lombok.RequiredArgsConstructor;

/**
 * Defines a binding as target for return bindings emitted in lower frames.
 */
@RequiredArgsConstructor
public class Assignment extends Frame {

    private final Binding target;

    @Override
    public Frame returnBinding(Binding binding) {
        binding.addOutput(target);
        return this;
    }
}
