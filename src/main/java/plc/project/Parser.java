package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        while (peek("LET")) {
            fields.add(parseField());
        }
        List<Ast.Method> methods = new ArrayList<>();
        while (peek("DEF")) {
            methods.add(parseMethod());
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        require("LET");
        String name = require(Token.Type.IDENTIFIER);
        Optional<Ast.Expr> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        require(";");
        return new Ast.Stmt.Field(name, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        require("DEF");
        String name = require(Token.Type.IDENTIFIER);
        List<String> parameters = new ArrayList<>();
        List<Ast.Stmt> statements = new ArrayList<>();
        require("(");
        if (peek(Token.Type.IDENTIFIER)) {
            do {
                parameters.add(getPreviousTokenLiteral());
            } while (match(","));
        }
        require(")");
        require("DO");
        while (!match("END")) {
            statements.add(parseStatement());
        }
        return new Ast.Method(name, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        Ast.Stmt result;
        if (peek("LET")) {
            result = parseDeclarationStatement();
        } else if (peek("IF")) {
            result = parseIfStatement();
        } else if (peek("FOR")) {
            result = parseForStatement();
        } else if (peek("WHILE")) {
            result = parseWhileStatement();
        } else if (peek("RETURN")) {
            result = parseReturnStatement();
        } else {
            Ast.Expr expression = parseExpression();
            if (match("=")) {
                result = new Ast.Stmt.Assignment(expression, parseExpression());
            } else {
                result = new Ast.Stmt.Expression(expression);
            }
            require(";");
        }
        return result;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        require("LET");
        String name = require(Token.Type.IDENTIFIER);
        Optional<Ast.Expr> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        require(";");
        return new Ast.Stmt.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        require("IF");
        Ast.Expr value = parseExpression();
        require("DO");
        List<Ast.Stmt> thenStatements = new ArrayList<>();
        List<Ast.Stmt> elseStatements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
        }
        require("END");
        return new Ast.Stmt.If(value, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        require("FOR");
        String name = require(Token.Type.IDENTIFIER);
        require("IN");
        Ast.Expr value = parseExpression();
        require("DO");
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!match("END")) {
            statements.add(parseStatement());
        }
        return new Ast.Stmt.For(name, value, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        require("WHILE");
        Ast.Expr condition = parseExpression();
        require("DO");
        List<Ast.Stmt> statements = new ArrayList<>();
        while (!match("END")) {
            statements.add(parseStatement());
        }
        return new Ast.Stmt.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        Ast.Stmt.Return result;
        require("RETURN");
        result =  new Ast.Stmt.Return(parseExpression());
        require(";");
        return result;
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr result = parseComparisonExpression();
        if (peek("AND") || peek("OR")) {
            while (match("AND") || match("OR")) {
                result = new Ast.Expr.Binary(getPreviousTokenLiteral(), result, parseComparisonExpression());
            }
        }
        return result;
    }

    public Ast.Expr parseComparisonExpression() throws ParseException {
        Ast.Expr result = parseAdditiveExpression();
        while (match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=")) {
            result = new Ast.Expr.Binary(getPreviousTokenLiteral(), result, parseAdditiveExpression());
        }
        return result;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr result = parseMultiplicativeExpression();
        while (match("+") || match("-")) {
            result = new Ast.Expr.Binary(getPreviousTokenLiteral(), result, parseMultiplicativeExpression());
        }
        return result;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr result = parseSecondaryExpression();
        while (match("*") || match("/")) {
            result = new Ast.Expr.Binary(getPreviousTokenLiteral(), result, parseSecondaryExpression());
        }
        return result;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr result = parsePrimaryExpression();
        while (match(".")) {
            String literal = require(Token.Type.IDENTIFIER);
            if (match("(")) {
                result = parseFunction(Optional.of(result), literal);
            } else {
                result = new Ast.Expr.Access(Optional.of(result), literal);
            }
        }
        return result;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        Ast.Expr result;
        if (match("NIL")) {
            result = new Ast.Expr.Literal(null);
        } else if (match("TRUE")) {
            result = new Ast.Expr.Literal(true);
        } else if (match("FALSE")) {
            result = new Ast.Expr.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            result = new Ast.Expr.Literal(new BigInteger(getPreviousTokenLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            result = new Ast.Expr.Literal(new BigDecimal(getPreviousTokenLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            result = new Ast.Expr.Literal(stripUnescape(getPreviousTokenLiteral()).charAt(0));
        } else if (match(Token.Type.STRING)) {
            result = new Ast.Expr.Literal(stripUnescape(getPreviousTokenLiteral()));
        } else if (match("(")) {
            result = new Ast.Expr.Group(parseExpression());
            require(")");
        } else if (match(Token.Type.IDENTIFIER)) {
            String literal = getPreviousTokenLiteral();
            if (match("(")) {
                result = parseFunction(Optional.empty(), literal);
            } else {
                result = new Ast.Expr.Access(Optional.empty(), literal);
            }
        } else {
            throw new ParseException("Invalid Primary Expression", tokens.get(0).getIndex());
        }
        return result;
    }

    private Ast.Expr parseFunction(Optional<Ast.Expr> receiver, String literal) {
        List<Ast.Expr> arguments = new ArrayList<>();
        if (!peek(")")) {
            do {
                arguments.add(parseExpression());
            } while (match(","));
        }
        require(")");
        return new Ast.Expr.Function(receiver, literal, arguments);
    }

    private String require(Object pattern) {
        if (match(pattern)) {
            return getPreviousTokenLiteral();
        } else {
            if (tokens.has(0)) {
                throw new ParseException(String.format("Expected '%s', received: ", pattern), tokens.get(0).getIndex());
            } else {
                throw new ParseException(String.format("Missing '%s'!", pattern), tokens.get(-1).getIndex() + getPreviousTokenLiteral().length());
            }
        }
    }
    
    private String getPreviousTokenLiteral() {
        return tokens.get(-1).getLiteral();
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

    private static String stripUnescape(String string) {
        return string.substring(1, string.length() - 1)
                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\'", "'")
                .replaceAll("\\\\b", "\b")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\\\t", "\t")
                .replaceAll("\\\\\\\\", "\\")
                ;
    }

}
