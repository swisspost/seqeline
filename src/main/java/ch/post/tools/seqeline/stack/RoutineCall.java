package ch.post.tools.seqeline.stack;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.binding.BindingType;

public class RoutineCall extends Frame {

    Binding returned = null;

    @Override
    public void returnBinding(Binding binding) {
        switch (binding.getType()) {
            case ROUTINE, CURSOR -> {
                if(returned == null) {
                    returned = new Binding(binding.getName(), BindingType.CALL);
                    returned.addReference(binding);
                    parent.returnBinding(returned);
                }
            }
            case VARIABLE, FIELD, PARAMETER, ALIAS, CALL -> {
                returned = binding;
                parent.returnBinding(binding); // function call is actually array dereferencing or special function (trim)
            }
            case ARGUMENT -> binding.addOutput(returned);

            default -> throw new IllegalStateException("Unexpected binding type");
        }
    }
}
