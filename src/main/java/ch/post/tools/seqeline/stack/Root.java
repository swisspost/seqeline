package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.Getter;

import java.util.Optional;

public class Root extends Frame {

    @Getter
    private final BindingBag bindings = new BindingBag();

    @Override
    protected Optional<Binding> resolveLocal(QualifiedName qualifiedName) {
        if(qualifiedName.isGlobal()) {
            var binding = new Binding(qualifiedName.getName(), BindingType.UNDEFINED);
            if (qualifiedName.getPrefix() != null) {
                var parent = new Binding(qualifiedName.getPrefix(), BindingType.UNDEFINED);
                binding = bindings.add(parent).addChild(binding);
            } else {
                binding = bindings.add(binding);
            }
            return Optional.of(binding);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void returnBinding(Binding binding) {
        var parent = new Binding("returned-orphans", BindingType.UNDEFINED);
        bindings.add(parent).addChild(binding);
    }

    @Override
    public Binding declare(Binding binding) {
        return bindings.add(binding);
    }
}
