package ch.post.tools.seqeline;

import ch.post.tools.seqeline.metadata.Schema;
import ch.post.tools.seqeline.parser.Parser;
import ch.post.tools.seqeline.process.TreeProcessor;
import lombok.SneakyThrows;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationTest {

    @Test
    public void testPackageBody() {
        var model = execute("""              
                CREATE OR REPLACE PACKAGE BODY test_pack IS
                    PROCEDURE test_proc (test_param IN NUMBER DEFAULT NULL) IS
                    BEGIN
                        test_pack2.test_proc2(test_param);
                    END;
                END;
                """);
        //model.stream().forEach(System.out::println);
        assertTrue(model.contains(iri(line_data, "test_pack"), RDF.TYPE, iri(line, "Package")));
    }

    private static Schema schema = new Schema();
    private Parser parser = new Parser();
    private String line ="https://schema.domain/lineage/";
    private String line_data ="https://data.domain/app/lineage/";

    @BeforeAll
    public static void init() {
        schema.populate(new ByteArrayInputStream(schema().getBytes()));
    }

    @SneakyThrows
    private Model execute(String source) {
        var root = parser.parse(new ByteArrayInputStream(source.getBytes()));
        var treeProcessor = new TreeProcessor("domain", "app", "file", root, schema);
        return treeProcessor.createModel();
    }

    private static String schema() {
        return """
            {
                "relations": [
                    {
                        "name": "employee",
                        "type": "table",
                        "comment": "Our workforce",
                        "columns": [
                            {
                                "name": "firstname",
                                "type": "varchar2"
                            },
                            {
                                "name": "lastname",
                                "type": "varchar2"
                            },
                            {
                                "name": "dept_id",
                                "type": "number",
                                "comment": "Foreign key to departments"
                            }
                        ]
                    },
                    {
                        "name": "department",
                        "type": "view",
                        "columns": [
                            {
                                "name": "id",
                                "type": "number"
                            },
                            {
                                "name": "name",
                                "type": "varchar2"
                            }
                        ]
                    } 
                ]
            }
            """;
    }
}