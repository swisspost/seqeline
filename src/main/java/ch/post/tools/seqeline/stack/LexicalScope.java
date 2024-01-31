package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import lombok.*;

import java.util.Optional;

@Getter
@Setter
@Builder
public class LexicalScope extends Frame {

    private final BindingBag bindings = new BindingBag();

    @Override
    protected Optional<Binding> resolveLocal(Reference reference) {
        return bindings.lookup(reference);
    }
}
