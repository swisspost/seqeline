package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingBag;
import ch.post.tools.seqeline.binding.BindingType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class Result extends Frame {

    private final BindingBag bindings = new BindingBag();

    @Override
    public Frame returnBinding(Binding binding) {
        var result = new Binding(binding.getName(), BindingType.RESULT);
        binding.addOutput(result);
        parent.returnBinding(result);
        return this;
    }
}