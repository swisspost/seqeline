package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class Expression extends Frame {

    private final BindingBag bindings = new BindingBag();

    @Override
    public Frame declare(Binding binding) {
        bindings.add(binding);
        return this;
    }

    @Override
    public Frame returnBinding(Binding binding) {
        bindings.add(binding);
        return this;
    }

    @Override
    protected void pop() {
        bindings.stream().forEach(parent::returnBinding);
    }
}