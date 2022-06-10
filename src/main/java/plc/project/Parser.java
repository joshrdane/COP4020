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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        Ast.Stmt result;
        Ast.Expr left = parseExpression();
        if (match("=")) {
            result = new Ast.Stmt.Assignment(left, parseExpression());
        } else {
            result = new Ast.Stmt.Expression(left);
        }
        if (!match(";")) {
            throw new ParseException("Expected ';', recevied: ", tokens.index + 1);
        }
        return result;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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
        while (match("AND") || match("OR")) {
            result = new Ast.Expr.Binary(tokens.get(-1).getLiteral(), result, parseComparisonExpression());
        }
        return result;
    }

    public Ast.Expr parseComparisonExpression() throws ParseException {
        Ast.Expr result = parseAdditiveExpression();
        while (match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=")) {
            result = new Ast.Expr.Binary(tokens.get(-1).getLiteral(), result, parseAdditiveExpression());
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
            result = new Ast.Expr.Binary(tokens.get(-1).getLiteral(), result, parseMultiplicativeExpression());
        }
        return result;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr result = parseSecondaryExpression();
        while (match("*") || match("/")) {
            result = new Ast.Expr.Binary(tokens.get(-1).getLiteral(), result, parseSecondaryExpression());
        }
        return result;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr result = parsePrimaryExpression();
        while (match(".")) {
            if (match(Token.Type.IDENTIFIER)) {
                String literal = tokens.get(-1).getLiteral();
                if (match("(")) {
                    result = parseFunction(Optional.of(result), literal);
                } else {
                    result = new Ast.Expr.Access(Optional.of(result), literal);
                }
            } else {
                throw new ParseException("Expected Identifier", -1);
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
            result = new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            result = new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            result = new Ast.Expr.Literal(stripUnescape(tokens.get(-1).getLiteral()).charAt(0));
        } else if (match(Token.Type.STRING)) {
            result = new Ast.Expr.Literal(stripUnescape(tokens.get(-1).getLiteral()));
        } else if (match("(")) {
            result = new Ast.Expr.Group(parseExpression());
            if (!match(")")) {
                throw new ParseException("Missing right paren", tokens.index + 1);
            }
        } else if (match(Token.Type.IDENTIFIER)) {
            String literal = tokens.get(-1).getLiteral();
            if (match("(")) {
                result = parseFunction(Optional.empty(), literal);
            } else {
                result = new Ast.Expr.Access(Optional.empty(), literal);
            }
        } else {
            throw new ParseException("Invalid Primary Expression", tokens.index);
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
        if (!match(")")) {
            throw new ParseException("Missing right paren", tokens.index + 1);
        }
        return new Ast.Expr.Function(receiver, literal, arguments);
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
