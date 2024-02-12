package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;

public class RoutineCall extends Frame {

    Binding routine = null;

    @Override
    public void returnBinding(Binding binding) {
        switch (binding.getType()) {
            case ROUTINE -> {
                routine = binding;
                parent.returnBinding(routine);
            }
            case ARGUMENT -> binding.addOutput(routine);
            default -> throw new IllegalStateException("Unexpected binding type");
        }
    }
}
