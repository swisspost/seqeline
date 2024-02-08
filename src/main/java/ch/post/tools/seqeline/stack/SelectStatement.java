package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
public class SelectStatement extends Frame {

    private final BindingBag selection = new BindingBag();

    private final BindingBag inputs = new BindingBag();

    private final BindingBag outputs = new BindingBag();

    private Phase phase = Phase.OUTPUTS;

    private enum Phase {
        OUTPUTS,
        INPUTS,
        EFFECTS
    }

    @Override
    protected Optional<Binding> resolveLocal(QualifiedName qualifiedName) {
        if(qualifiedName.isFunctional()) {
            return selection.lookup(qualifiedName);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Binding declare(Binding binding) {
        return selection.add(binding);
    }

    @Override
    public void returnBinding(Binding returned) {
        switch (phase) {
            case OUTPUTS -> {
                parent.returnBinding(returned);
                outputs.add(returned);
            }
            case INPUTS -> (returned.getType() == BindingType.RELATION ? returned.children() : Stream.of(returned))
                    .forEach(binding -> {
                        selection.get(binding).ifPresent(binding::addOutput);
                        inputs.add(binding);
                    });
            case EFFECTS -> {
                if (returned.getType() == BindingType.RELATION) {
                    returned.children().forEach(binding -> {
                        selection.get(binding).ifPresent(binding::addOutput);
                        inputs.add(binding);
                    });
                } else {
                    var cause = inputs.get(returned)
                            .or(() -> outputs.get(returned))
                            .or(() -> parent.resolve(QualifiedName.of(null, returned.getName())))
                            .orElseThrow();
                    outputs.stream().forEach(cause::addEffect);
                }
            }
        }
    }

    public static class SelectList extends Frame {
        @Override
        public void returnBinding(Binding binding) {
            var result = new Binding(binding.getName(), BindingType.RESULT);
            binding.addOutput(result);
            parent.returnBinding(result);
        }

        @Override
        protected void pop() {
            ((SelectStatement) parent).phase = Phase.INPUTS;
        }
    }

    public static class EffectClause extends Frame {

        BindingBag fields = new BindingBag();

        @Override
        public Binding declare(Binding binding) {
            return fields.add(binding);
        }

        @Override
        protected void push() {
            ((SelectStatement) parent).phase = Phase.EFFECTS;
        }

    }
}