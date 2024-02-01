package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.*;

import java.util.Optional;

public abstract class Frame {
    @Setter
    protected Frame parent;

    public Optional<Binding> resolve(QualifiedName qualifiedName) {
        return resolveLocal(qualifiedName)
                .or(() -> Optional.ofNullable(parent).flatMap(p -> p.resolve(qualifiedName)));
    }

    public void returnBinding(Binding binding) {
        Optional.ofNullable(parent).ifPresent(p -> p.returnBinding(binding));
    }

    protected Optional<Binding> resolveLocal(QualifiedName qualifiedName) {
        return Optional.empty();
    }

    public void declare(Binding binding) {
        Optional.ofNullable(parent).orElseThrow().declare(binding);
    }
}
