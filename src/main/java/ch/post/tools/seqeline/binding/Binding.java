package ch.post.tools.seqeline.binding;

import ch.post.tools.seqeline.stack.QualifiedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
@EqualsAndHashCode
public class Binding {

    @Getter
    private final String name;

    @Getter
    private final BindingType type;

    private final Map<String, Binding> children = new HashMap<>();

    private final Set<Binding> outputs = new HashSet<>();

    public Optional<Binding> match(QualifiedName qualifiedName) {
        if(name.equals(qualifiedName.getPrefix())) {
            return Optional.ofNullable(children.get(name));
        } else if(qualifiedName.getPrefix() == null && name.equals(qualifiedName.getName())) {
            return Optional.of(this);
        } else {
            return Optional.empty();
        }
    }

    public Binding addChild(Binding binding) {
        return children.putIfAbsent(binding.getName(), binding);
    }

    public void addOutput(Binding binding) {
        outputs.add(binding);
    }

    public Stream<Binding> outputs() {
        return outputs.stream();
    }

    public Stream<Binding> children() {
        return children.values().stream();
    }
}
