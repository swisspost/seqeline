package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Defines a binding as parent for return bindings emitted in lower frames.
 */
@RequiredArgsConstructor
public class Children extends Frame {

    private Binding owner;

    private boolean returns = false;

    private boolean receivedChildren = false;

    public Children(Binding owner) {
        this.owner = owner;
    }

    public Children(boolean returns) {
        this.returns = returns;
    }

    @Override
    public void returnBinding(Binding binding) {
        if(owner != null && owner.getType() == BindingType.FIELD && binding.getType() == BindingType.CALL) {
            // Replace owner with package
            owner = parent.resolve(new QualifiedName(null, owner.getName(), false, BindingType.PACKAGE)).orElseThrow();
            binding = binding.references().findFirst().orElseThrow();
        }
        if(owner == null) {
            owner = binding;
        } else {
            if (binding.getType() != BindingType.RELATION) {
                owner.addChild(binding);
                receivedChildren = true;
            }
            if(returns) {
                parent.returnBinding(binding);
            }
        }
    }

    @Override
    protected void pop() {
        if(owner != null && !receivedChildren) {
            parent.returnBinding(owner);
        }
    }
}
