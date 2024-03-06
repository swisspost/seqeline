package ch.post.tools.seqeline;

import ch.post.tools.seqeline.catalog.Schema;
import ch.post.tools.seqeline.process.TreeProcessor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.antlr.grammars.plsql.PlSqlLexer;
import org.antlr.grammars.plsql.PlSqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.commons.text.StringEscapeUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Set;

@Log
public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        if(args.length > 0 && args[0].equals("-p")) {

            InputStream inputStream = new FileInputStream(args[1]);

            var lexer = new PlSqlLexer(CharStreams.fromStream(inputStream));
            var tokenStream = new CommonTokenStream(lexer);
            var parser = new PlSqlParser(tokenStream);
            var parseTree = parser.sql_script();
            var walker = new ParseTreeWalker();

            var out = args.length > 2 ?  new FileOutputStream(args[2]) : System.out;
            var writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write("<?xml version=\"1.0\"?>");
            writer.newLine();
            var listener = new ParseTreeListener() {

                private String indent =  "";
                private boolean showTerminal = false;
                private int ignoreDepth = 0;

                @SneakyThrows
                @Override
                public void visitTerminal(TerminalNode terminalNode) {
                    if(showTerminal) {
                        writer.write(indent + StringEscapeUtils.escapeXml11(terminalNode.toString()));
                        writer.newLine();
                    }
                }

                @Override
                public void visitErrorNode(ErrorNode errorNode) {
                    throw new RuntimeException(errorNode.toString());
                }

                @SneakyThrows
                @Override
                public void enterEveryRule(ParserRuleContext parserRuleContext) {
                    var name = name(parser, parserRuleContext);
                    if(ignore(name)) {
                        ignoreDepth++;
                    }
                    if(ignoreDepth == 0) {
                        if (show(name)) {
                            writer.write(indent + "<" + name + ">");
                            writer.newLine();
                            indent = indent + "  ";
                        }
                    }
                    if(terminal(name)) {
                        showTerminal = true;
                    }
                }

                @SneakyThrows
                @Override
                public void exitEveryRule(ParserRuleContext parserRuleContext) {
                    var name = name(parser, parserRuleContext);
                    if(ignoreDepth == 0) {
                        if (show(name)) {
                            indent = indent.substring(0, indent.length() - 2);
                            writer.write(indent + "</" + name + ">");
                            writer.newLine();
                        }
                    }
                    if(ignore(name)) {
                        ignoreDepth--;
                    }
                    if(terminal(name)) {
                        showTerminal = false;
                    }
                }
            };
            walker.walk(listener, parseTree);
            writer.flush();

        } else {
            if (args.length < 3) {
                log.severe("seqeline <domain> <application> <filename>");
                System.exit(1);
            }
            Schema schema = new Schema("data/model/metadata.json");
            new TreeProcessor(args[0], args[1], args[2], schema).process("target/out.trig");
        }
    }

    private static String name(PlSqlParser parser, ParserRuleContext parserRuleContext) {
        return parserRuleContext.toString(parser, parserRuleContext.getParent())
                .replaceAll("[\\[\\]]", "")
                .trim();
    }

    private static boolean show(String name) {
        return !skipped.contains(name);
    }

    private static boolean ignore(String name) {
        return ignored.contains(name);
    }

    private static boolean terminal(String name) {
        return terminal.contains(name);
    }

    private static Set<String> terminal = Set.of(
            "id_expression",
            "constant"
    );

    private static Set<String> ignored = Set.of(
            "type_spec",
            "exception_declaration"
    );

    private static Set<String> skipped = Set.of(
            "unit_statement",
            "package_obj_spec",
            "package_obj_body",
            "atom",
            "standard_function",
            "seq_of_declare_specs",
            "declare_spec",
            "select_statement",

            "regular_id",
            "non_reserved_keywords_pre12c",

            "model_expression",
            "logical_expression",
            "multiset_expression",
            "unary_logical_expression",
            "relational_expression",
            "compound_expression",
            "unary_expression",
            "concatenation",

            "quoted_string",
            "numeric",

            "function_argument",

            "transaction_control_statements",
            "commit_statement"
    );
}
