package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.*;

import java.lang.reflect.Field;
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
        var localBinding = declarations.lookup(qualifiedName);;
        if(qualifiedName.getType() == BindingType.RELATION || qualifiedName.getType() == BindingType.PACKAGE) {
            localBinding.ifPresent(declarations::remove);
            return Optional.empty();
        } else {
            return localBinding;
        }
    }

    @Override
    public void returnBinding(Binding binding) {
        if(binding.getType() == BindingType.FIELD && binding.getName().equals("delete")) {
            // array deletion, ignore
            return;
        }
        switch(binding.getType()) {
            case CALL -> { /* don't record routine calls */ }
            case RETURN -> {
                if(owner != null) {
                    binding.addOutput(owner);
                } else {
                    parent.returnBinding(binding);
                }
            }
            default ->  parent.returnBinding(binding);
        }
     }

    @Override
    public Binding declare(Binding binding) {
        Binding result;
        // Let global objects bubble up
        if(binding.getType() == BindingType.RELATION || binding.getType() == BindingType.PACKAGE) {
            result = super.declare(binding);
        } else {
            result = declarations.add(binding);
        }
        if(owner != null &&
                (binding.getType() == BindingType.ROUTINE ||
                        binding.getType() == BindingType.PARAMETER ||
                        binding.getType() == BindingType.CURSOR ||
                        binding.getType() == BindingType.VARIABLE
                )) {
            owner.addChild(binding);
        }
        return result;
    }
}
