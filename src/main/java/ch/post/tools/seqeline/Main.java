package ch.post.tools.seqeline;

import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.joox.Match;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static org.joox.JOOX.$;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        if(args.length < 1) {
            System.err.println("Missing filename argument");
            System.exit(1);
        }

        $(new File(args[0]))
                .find()
                .each()
                .stream()
                .map(Match::xpath)
                .forEach(System.out::println);

        ModelBuilder modelBuilder = new ModelBuilder();
    }
}
