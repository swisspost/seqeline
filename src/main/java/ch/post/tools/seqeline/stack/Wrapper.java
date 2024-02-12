package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;

public class Wrapper extends Frame {

    private final Binding wrapper;

    public Wrapper(Binding wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public void returnBinding(Binding binding) {
        binding.addOutput(wrapper);
    }

    @Override
    protected void pop() {
        parent.returnBinding(wrapper);
    }
}
