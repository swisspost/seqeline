package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import lombok.*;

import java.util.Optional;

public abstract class Frame {
    @Setter
    protected Frame parent;

    public Optional<Binding> resolve(Reference reference) {
        return resolveLocal(reference)
                .or(() -> Optional.ofNullable(parent).flatMap(p -> p.resolve(reference)));
    }

    public void returnBinding(Binding binding) {
        Optional.ofNullable(parent).ifPresent(p -> p.returnBinding(binding));
    }

    protected Optional<Binding> resolveLocal(Reference reference) {
        return Optional.empty();
    }
}
