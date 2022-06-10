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
        if (match("LET")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                Optional<Ast.Expr> value = Optional.empty();
                if (match("=")) {
                    value = Optional.of(parseExpression());
                }
                if (!match(";")) {
                    throw new ParseException("Expected ';', received: ", tokens.index);
                }
                return new Ast.Stmt.Field(name, value);
            } else {
                throw new ParseException("Expected Identifier, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible Declaration Statement");
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (match("DEF")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                List<String> parameters = new ArrayList<>();
                List<Ast.Stmt> statements = new ArrayList<>();
                if (match("(")) {
                    while (match(Token.Type.IDENTIFIER)) {
                        parameters.add(tokens.get(-1).getLiteral());
                        if (!match(",")) {
                            break;
                        }
                    }
                    if (!match(")")) {
                        throw new ParseException("Missing right paren", tokens.index);
                    }
                    if (!match("DO")) {
                        throw new ParseException("Expected DO, received: ", tokens.index);
                    }
                    while (!match("END")) {
                        statements.add(parseStatement());
                    }
                    return new Ast.Method(name, parameters, statements);
                } else {
                    throw new ParseException("Expected '(', received: ", tokens.index);
                }
            } else {
                throw new ParseException("Expected Identifier, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible Declaration Statement");
        }
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
        if (match("LET")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                Optional<Ast.Expr> value = Optional.empty();
                if (match("=")) {
                    value = Optional.of(parseExpression());
                }
                if (!match(";")) {
                    throw new ParseException("Expected ';', received: ", tokens.index);
                }
                return new Ast.Stmt.Declaration(name, value);
            } else {
                throw new ParseException("Expected Identifier, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible Declaration Statement");
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        if (match("IF")) {
            Ast.Expr value = parseExpression();
            if (match("DO")) {
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
                if (!match("END")) {
                    throw new ParseException("Expected END, received: ", tokens.index);
                }
                return new Ast.Stmt.If(value, thenStatements, elseStatements);
            } else {
                throw new ParseException("Expected DO, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible If Statement");
        }
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        if (match("FOR")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                if (match("IN")) {
                    Ast.Expr value = parseExpression();
                    if (match("DO")) {
                        List<Ast.Stmt> statements = new ArrayList<>();
                        while (!match("END")) {
                            statements.add(parseStatement());
                        }
                        return new Ast.Stmt.For(name, value, statements);
                    } else {
                        throw new ParseException("Expected DO, received: ", tokens.index);
                    }
                } else {
                    throw new ParseException("Expected IN, received: ", tokens.index);
                }
            } else {
                throw new ParseException("Expected Identifier, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible For Statement");
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        if (match("WHILE")) {
            Ast.Expr condition = parseExpression();
            if (match("DO")) {
                List<Ast.Stmt> statements = new ArrayList<>();
                while (!match("END")) {
                    statements.add(parseStatement());
                }
                return new Ast.Stmt.While(condition, statements);
            } else {
                throw new ParseException("Expected DO, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible While Statement");
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        if (match("RETURN")) {
            if (match(Token.Type.IDENTIFIER)) {
                return new Ast.Stmt.Return(parseExpression());
            } else {
                throw new ParseException("Expected Identifier, received: ", tokens.index);
            }
        } else {
            throw new RuntimeException("Reached impossible Return Statement");
        }
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
