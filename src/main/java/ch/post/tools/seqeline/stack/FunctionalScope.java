package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.*;

import java.util.stream.Stream;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class FunctionalScope extends Frame {

    private final BindingBag declarations = new BindingBag();

    private final BindingBag inputs = new BindingBag();

    @Override
    public Frame declare(Binding binding) {
        declarations.add(binding);
        return this;
    }

    @Override
    public Frame returnBinding(Binding input) {
        (input.getType() == BindingType.RELATION ? input.children() : Stream.of(input))
                .forEach(binding -> {
            declarations.get(binding).ifPresent(binding::addOutput);
            inputs.add(binding);
        });
        return this;
    }

    @Override
    protected void pop() {
        // Resolve and attach all declarations that have not been assigned from the source
        declarations.stream()
                .filter(declaration -> inputs.get(declaration).isEmpty())
                .forEach(declaration ->
                        parent.resolve(QualifiedName.of(null, declaration.getName(), false))
                                .ifPresentOrElse(declaration::addOutput,
                                        () -> {
                                            // Bubble up unresolved relation-like bindings
                                            if(declaration.getType() == BindingType.RELATION) {
                                                parent.declare(declaration);
                                            }
                                        }));
        inputs.stream()
                .filter(input -> input.getType() == BindingType.RESULT)
                .forEach(parent::returnBinding);
    }

}