package ch.post.tools.seqeline.binding;

import ch.post.tools.seqeline.binding.Binding;
import ch.post.tools.seqeline.stack.Reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BindingBag {

    private final Map<String, Binding> bindings = new HashMap<>();

    public Optional<Binding> lookup(Reference reference) {
        String root = Optional.ofNullable(reference.getPrefix())
                .orElse(reference.getName());
        return Optional.ofNullable(bindings.get(root))
                .flatMap(binding -> binding.match(reference));
    }

    public Binding add(Binding binding) {
        return bindings.putIfAbsent(binding.getName(), binding);
    }
}
