package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Defines a binding as target for return bindings emitted in lower frames.
 */
@RequiredArgsConstructor
@AllArgsConstructor
public class Assignment extends Frame {

    private Binding target;

    @Override
    public void returnBinding(Binding binding) {
        if(binding.getType() != BindingType.RELATION) {
            if(target == null) {
                target = binding;
            } else {
                binding.addOutput(target);
            }
        }
    }
}
