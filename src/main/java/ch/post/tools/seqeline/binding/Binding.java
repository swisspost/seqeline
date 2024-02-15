package ch.post.tools.seqeline.binding;

import ch.post.tools.seqeline.stack.QualifiedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Binding {

    private final static AtomicInteger sequence = new AtomicInteger(0);

    @Getter
    private final int id = sequence.incrementAndGet();

    @Getter
    private final String name;

    @Getter
    private final BindingType type;

    @Getter
    @Setter
    private String globalName;

    @Getter
    private Integer position = null;

    private final Map<String, Binding> children = new HashMap<>();

    private final Set<Binding> outputs = new HashSet<>();

    private final Set<Binding> effects = new HashSet<>();

    private final Set<Binding> references = new HashSet<>();

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
        children.putIfAbsent(binding.getName(), binding);
        return children.get(binding.getName());
    }

    public void addOutput(Binding binding) {
        outputs.add(binding);
    }

    public void addEffect(Binding binding) {
        effects.add(binding);
    }

    public void addReference(Binding binding) {
        references.add(binding);
    }

    public Stream<Binding> children() {
        return children.values().stream();
    }

    public Stream<Binding> outputs() {
        return outputs.stream();
    }

    public Stream<Binding> effects() {
        return effects.stream();
    }

    public Stream<Binding> references() {
        return references.stream();
    }

    public Binding position(int position) {
        this.position = position;
        return this;
    }
}
