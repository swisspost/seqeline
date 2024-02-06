package ch.post.tools.seqeline;

import ch.post.tools.seqeline.catalog.Schema;
import ch.post.tools.seqeline.process.TreeProcessor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.joox.Context;
import org.joox.Match;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.catalog.Catalog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.joox.JOOX.$;

@Log
public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        if (args.length < 3) {
            log.severe("seqeline <domain> <application> <filename>");
            System.exit(1);
        }
        Schema schema = new Schema("data/model/metadata.json");
        new TreeProcessor(args[0], args[1], args[2], schema).process("target/out.trig");
    }
}
