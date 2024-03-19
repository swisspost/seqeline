package ch.post.tools.seqeline.parser;

public class ParseException extends RuntimeException {
    private boolean skipped;

    public ParseException() {

    }

    public ParseException(boolean skip) {
        this.skipped = skip;
    }

    public boolean isSkipped() {
        return skipped;
    }
}
