package ch.post.tools.seqeline.binding;

import ch.post.tools.seqeline.stack.QualifiedName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class BindingBag {

    private final Map<String, Binding> bindings = new HashMap<>();

    public Optional<Binding> lookup(QualifiedName qualifiedName) {
        String root = Optional.ofNullable(qualifiedName.getPrefix())
                .orElse(qualifiedName.getName());
        var result = Optional.ofNullable(bindings.get(root))
                .flatMap(binding -> binding.match(qualifiedName));
        return result;
    }

    public Binding add(Binding binding) {
        bindings.putIfAbsent(binding.getName(), binding);
        return bindings.get(binding.getName());
    }

    public Optional<Binding> get(Binding binding) {
        return Optional.ofNullable(bindings.get(binding.getName()));
    }

    public Optional<Binding> remove(Binding binding) {
        return Optional.ofNullable(bindings.remove(binding.getName()));
    }

    public Stream<Binding> stream() {
        return bindings.values().stream();
    }
}
