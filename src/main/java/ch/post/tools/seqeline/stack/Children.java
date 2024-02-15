package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Defines a binding as target for return bindings emitted in lower frames.
 */
@RequiredArgsConstructor
public class Children extends Frame {

    private final Binding parent;

    @Override
    public void returnBinding(Binding binding) {
        if(binding.getType() != BindingType.RELATION) {
            parent.addChild(binding);
        }
    }
}
