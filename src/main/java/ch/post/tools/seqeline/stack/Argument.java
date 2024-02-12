package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;

public class Argument extends Frame {

    private String name;

    public Argument(String name) {
        this.name = name;
    }

    @Override
    public void returnBinding(Binding binding) {
        var argument = new Binding(name, BindingType.ARGUMENT);
        binding.addOutput(argument);
        super.returnBinding(argument);
    }
}
