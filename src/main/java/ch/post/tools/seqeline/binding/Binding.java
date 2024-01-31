package ch.post.tools.seqeline.binding;

import ch.post.tools.seqeline.stack.Reference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
@EqualsAndHashCode
public class Binding {

    @Getter
    private final String name;

    private final Map<String, Binding> children = new HashMap<>();

    private final Set<Binding> inputs = new HashSet<>();
    private final Set<Binding> outputs = new HashSet<>();

    public Optional<Binding> match(Reference reference) {
        if(name.equals(reference.getPrefix())) {
            return Optional.ofNullable(children.get(name));
        } else if(reference.getPrefix() == null && name.equals(reference.getName())) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    public void addChild(Binding binding) {
        children.putIfAbsent(binding.getName(), binding);
    }

    public void addInput(Binding binding) {
        inputs.add(binding);
    }

    public void addOutput(Binding binding) {
        outputs.add(binding);
    }
}
