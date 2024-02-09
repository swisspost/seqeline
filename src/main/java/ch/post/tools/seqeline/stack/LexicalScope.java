package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.*;

import java.util.Optional;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LexicalScope extends Frame {

    private final BindingBag declarations = new BindingBag();

    private Binding owner;

    @Override
    protected Optional<Binding> resolveLocal(QualifiedName qualifiedName) {
        return declarations.lookup(qualifiedName);
    }

    @Override
    public Binding declare(Binding binding) {
        Binding result = binding;
        // Let global objects bubble up
        if(binding.getType() == BindingType.RELATION || binding.getType() == BindingType.PACKAGE) {
            result = super.declare(binding);
        } else {
            result = declarations.add(binding);
        }
        if(owner != null &&
                (binding.getType() == BindingType.ROUTINE ||
                binding.getType() == BindingType.PARAMETER)) {
            owner.addChild(binding);
        }
        return result;
    }
}
