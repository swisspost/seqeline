package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;

public class Argument extends Frame {

    private final Binding argument;

    public Argument(String name) {
        argument = new Binding(name, BindingType.ARGUMENT);
    }

    @Override
    public void returnBinding(Binding binding) {
        binding.addOutput(argument);
    }

    @Override
    protected void pop() {
        parent.returnBinding(argument);
    }
}
