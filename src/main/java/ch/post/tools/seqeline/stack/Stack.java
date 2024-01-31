package ch.post.tools.seqeline.stack;

public class Stack {
    private final Frame root = new Root();
    private Frame top = root;

    public void execute(Frame frame, Runnable runnable) {
        frame.parent = top;
        top = frame;
        runnable.run();
        top = frame.parent;
    }

    public Frame top() {
        return top;
    }

    public Frame root() {
        return root;
    }
}