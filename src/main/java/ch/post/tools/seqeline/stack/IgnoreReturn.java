package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;

public class IgnoreReturn extends Frame {
    @Override
    public void returnBinding(Binding binding) {
        // do nothing
    }
}
