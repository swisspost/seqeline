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

    private Binding owner;

    private boolean returns = false;

    public Children(Binding owner) {
        this.owner = owner;
    }

    public Children(boolean returns) {
        this.returns = returns;
    }

    @Override
    public void returnBinding(Binding binding) {
        if(owner == null) {
            owner = binding;
        } else {
            if (binding.getType() != BindingType.RELATION) {
                owner.addChild(binding);
            }
            if(returns) {
                parent.returnBinding(binding);
            }
        }
    }

    @Override
    protected void pop() {
        if(owner != null && owner.children().findAny().isEmpty()) {
            parent.returnBinding(owner);
        }
    }
}
