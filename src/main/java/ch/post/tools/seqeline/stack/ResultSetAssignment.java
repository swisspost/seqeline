package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.RequiredArgsConstructor;

/**
 * Defines a bag of named binding as target for return bindings emitted in lower frames.
 * Returned columns binding with same name are attached directly to the corresponding target.
 * In all other cases, returned bindings are attached to each binding of the bag.
 */
@RequiredArgsConstructor
public class ResultSetAssignment extends Frame {

    private final BindingBag targets;

    @Override
    public void returnBinding(Binding binding) {
        var candidate = targets.lookup(QualifiedName.of(binding.getName()));
        if (binding.getType() == BindingType.COLUMN && candidate.isPresent()) {
            binding.addOutput(candidate.get());
        } else {
            targets.stream().forEach(binding::addOutput);
        }
    }
}
