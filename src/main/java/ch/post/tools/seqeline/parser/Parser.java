package ch.post.tools.seqeline.parser;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.antlr.grammars.plsql.PlSqlLexer;
import org.antlr.grammars.plsql.PlSqlParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.text.StringEscapeUtils;
import org.joox.Match;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import static org.joox.JOOX.$;

@Slf4j
public class Parser {

    private static final Pattern dollarLine = Pattern.compile("^\\s*\\$.*");
    private static final Pattern javaSource = Pattern.compile("^CREATE OR REPLACE AND RESOLVE JAVA.*");

    public Match parse(InputStream inputStream) throws IOException, SAXException {

        // Preprocess to ignore conditional compilation macros ($IF, ...)
        // Also aborts when Java source is encountered
        var scanner = new Scanner(new BOMInputStream(inputStream));
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine();
            if(!dollarLine.matcher(line).matches()) {
                sb.append(line);
            }
            sb.append("\n");
            if(javaSource.matcher(line).matches()) {
                throw new ParseException(true);
            }
        }

        var lexer = new PlSqlLexer(CharStreams.fromString(sb.toString()));
        List<String> errors = new ArrayList<>();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
                errors.add(s);
            }
        });
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new PlSqlParser(tokenStream);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
                errors.add(s);
            }
        });
        var parseTree = parser.sql_script();
        var walker = new ParseTreeWalker();

        var out = new ByteArrayOutputStream();
        var writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.newLine();
        var listener = new ParseTreeListener() {

            private String indent = "";
            private boolean showTerminal = false;
            private int ignoreDepth = 0;

            @SneakyThrows
            @Override
            public void visitTerminal(TerminalNode terminalNode) {
                if (showTerminal) {
                    writer.write(indent + StringEscapeUtils.escapeXml11(terminalNode.toString()));
                    writer.newLine();
                }
            }

            @Override
            public void visitErrorNode(ErrorNode errorNode) {
                errors.add(errorNode.toString());
            }


            @SneakyThrows
            @Override
            public void enterEveryRule(ParserRuleContext parserRuleContext) {
                var name = name(parser, parserRuleContext);
                if (ignore(name)) {
                    ignoreDepth++;
                }
                if (ignoreDepth == 0) {
                    if (show(name)) {
                        writer.write(indent + "<" + name + ">");
                        writer.newLine();
                        indent = indent + "  ";
                    }
                }
                if (terminal(name)) {
                    showTerminal = true;
                }
            }

            @SneakyThrows
            @Override
            public void exitEveryRule(ParserRuleContext parserRuleContext) {
                var name = name(parser, parserRuleContext);
                if (ignoreDepth == 0) {
                    if (show(name)) {
                        indent = indent.substring(0, indent.length() - 2);
                        writer.write(indent + "</" + name + ">");
                        writer.newLine();
                    }
                }
                if (ignore(name)) {
                    ignoreDepth--;
                }
                if (terminal(name)) {
                    showTerminal = false;
                }
            }
        };
        walker.walk(listener, parseTree);
        writer.flush();

        if(errors.isEmpty()) {
            return $(new ByteArrayInputStream(out.toByteArray()));
        } else {
            throw new ParseException();
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
