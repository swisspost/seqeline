package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import lombok.Getter;

import java.util.Optional;

public class Root extends Frame {

    @Getter
    private final BindingBag bindings = new BindingBag();

    @Override
    protected Optional<Binding> resolveLocal(Reference reference) {
        var binding = new Binding(reference.getName());
        if(reference.getPrefix() != null) {
            var parent = new Binding(reference.getPrefix());
            bindings.add(parent).addChild(binding);
        } else {
            bindings.add(binding);
        }
        return Optional.of(binding);
    }
}
