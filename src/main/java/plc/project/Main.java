package plc.project;

import java.util.List;
import java.util.Scanner;

/**
 * Place this file in your plc.project directory and
 * select the green arrow next to Main to run the program.
 *
 * Your source to be evaluated is entered at the prompt.
 * You can modify the solution to read source from a file.
 * You can also modify this solution to work with our Analyzer and Generator.
 * Note, you will either interpret or analyze+generate, but not both simultaneously,
 * (due to how we have designed our project implementation).
 *
 * Sample source:

 DEF main() DO print(1 + 2); END
 //

 * The inline comment with no information (no actual comment) closes the source entry.
 * This allows you to enter as many or as few lines as you would like to evaluate.
 *
 * This source example prints 3.
 *
 * In this solution, the AST is built from the source (parseSource),
 * Therefore, you must provide valid source from the root of the grammar.
 * You can modify the solution to start from other nodes and test more specific expressions,
 * such as by starting at statement (parseStatement) or expression (parseExpression) levels.
 */

public class Main {

    public static void main( String args[] ) {

        // scanner for prompt input of source
        Scanner scanner = new Scanner(System.in);
        String line, source = "";

        System.out.println("Running the Lexer through Interpreter...");
        System.out.println();

        System.out.println("Enter the Source (end with // by itself on the last source line):");
        line = scanner.nextLine().trim();          // enter source lines
        while ( !line.equals("//") ) {      // close out source at empty //
            source = source + line + "\n";  // build source
            line = scanner.nextLine();      // enter source lines
        }

        System.out.println();

        // lex source --> token list for the Parser
        List<Token> tokens = new Lexer(source).lex();
        
        // parse source --> AST for the Interpreter
        Ast.Source ast = new Parser(tokens).parseSource();

        // interpret source --> evaluate results
        Environment.PlcObject plc = new Interpreter(new Scope(null)).visit(ast);

        System.out.println();
        System.out.println("Any PLCObject Results:");
        System.out.println();

        System.out.println(plc);
    }
}
