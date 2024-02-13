package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;

public class RoutineCall extends Frame {

    Binding call = null;

    @Override
    public void returnBinding(Binding binding) {
        switch (binding.getType()) {
            case ROUTINE -> {
                if(call == null) {
                    call = new Binding(binding.getName(), BindingType.CALL);
                    call.addReference(binding);
                    parent.returnBinding(call);
                }
            }
            case ARGUMENT -> binding.addOutput(call);
            default -> throw new IllegalStateException("Unexpected binding type");
        }
    }
}
