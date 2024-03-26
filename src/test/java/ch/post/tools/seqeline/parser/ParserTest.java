package ch.post.tools.seqeline.parser;

import org.joox.Match;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    public void testSimpleCode() throws IOException, SAXException {
        var input = """
                select * from dual;
                """;
        Match root = new Parser().parse(new ByteArrayInputStream(input.getBytes()));
        assertFalse(root.find("select_only_statement").isEmpty());
        assertEquals("dual", root.find("id_expression").text().trim());
    }

    @Test
    public void testConditionalCompilation() throws IOException, SAXException {
        var input = """
                $IF debug
                select * from dual;
                $ELSE
                select * from dual;
                $END
                """;
        new Parser().parse(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    public void testJavaSource() throws IOException, SAXException {
        var input = """
            CREATE OR REPLACE AND RESOLVE JAVA SOURCE NAMED "Test"
            AS
                import java.io.*;
 
                public class Test {
                ...               
            """;
        try {
            new Parser().parse(new ByteArrayInputStream(input.getBytes()));
        } catch (ParseException e) {
            assertTrue(e.isSkipped());
        }
    }

}