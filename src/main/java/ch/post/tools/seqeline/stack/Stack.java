package ch.post.tools.seqeline.stack;

public class Stack {
    private final Root root = new Root();
    private Frame top = root;

    public void execute(Frame frame, Runnable runnable) {
        frame.parent = top;
        top = frame;
        frame.push();
        runnable.run();
        frame.pop();
        top = frame.parent;
    }

    public Frame top() {
        return top;
    }

    public Root root() {
        return root;
    }

    public void pop() {
        top.pop();
        top = top.parent;
    }
}
