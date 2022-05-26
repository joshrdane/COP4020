package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {
    private final static class RegexPattern {
        final static String BACKSLASH = "[\\\\]";
        final static String DIGIT = "[0-9]";
        final static String SINGLE_QUOTE = "[']";
        final static String DOUBLE_QUOTE = "[\"]";
        final static String PLUS_OR_MINUS = "[+|\\-]";
        final static String IDENTIFIER_INIT = "[A-Za-z_]";
        final static String IDENTIFIER_BODY = "[A-Za-z0-9_-]";
        final static String STRING = "[^\"\n\r]";
        final static String ESCAPE_BODY = "[bnrt'\"\\\\]";
        final static String WHITESPACE = "[ \b\n\r\t]";
        final static String NONWHITESPACE = "[^ \b\n\r\t]";
        final static String OPERATOR = "[<>!=]";
        final static String EQUAL = "[<>!=]";
        final static String PERIOD = "[\\.]";
    }

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        ArrayList<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (match(RegexPattern.WHITESPACE)) {
                chars.skip(); // advance past whitespace
            } else {
                tokens.add(lexToken()); // lex next token
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek(RegexPattern.IDENTIFIER_INIT)) {
            return lexIdentifier();
        } else if (peek(RegexPattern.DIGIT)
                || peek(RegexPattern.PLUS_OR_MINUS, RegexPattern.DIGIT)) {
            return lexNumber();
        } else if (peek(RegexPattern.SINGLE_QUOTE)) {
            return lexCharacter();
        } else if (peek(RegexPattern.DOUBLE_QUOTE)) {
            return lexString();
        } else {
            return lexOperator();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public Token lexIdentifier() {
        if (match(RegexPattern.IDENTIFIER_INIT)) {
            while (match(RegexPattern.IDENTIFIER_BODY));
            return chars.emit(Token.Type.IDENTIFIER);
        } else {
            throw new ParseException("Invalid start of Identifier: ", chars.index);
        }
    }

    public Token lexNumber() {
        if (match(RegexPattern.PLUS_OR_MINUS, RegexPattern.DIGIT) || match(RegexPattern.DIGIT)) {
            Token.Type type = Token.Type.INTEGER;
            do {
                if (type.equals(Token.Type.INTEGER) && match(RegexPattern.PERIOD, RegexPattern.DIGIT)) {
                    type = Token.Type.DECIMAL;
                }
            } while (match(RegexPattern.DIGIT));
            return chars.emit(type);
        } else {
            throw new ParseException("Invalid start to Number: ", chars.index);
        }
    }

    public Token lexCharacter() {
        String pattern = "[^'\n\r]";
        if (match(RegexPattern.SINGLE_QUOTE)) {
            if (peek(RegexPattern.BACKSLASH)) {
                lexEscape();
            } else if (!match(pattern)) { // consumes the character or throws an exception
                throw new ParseException("Invalid character: ", chars.index);
            }
            if (match(RegexPattern.SINGLE_QUOTE)) {
                return chars.emit(Token.Type.CHARACTER);
            } else if (peek(pattern)) { // this verifies there is indeed another character that is not: '
                throw new ParseException("Invalid length for Character literal: ", chars.index);
            } else {
                throw new ParseException("Missing: '", chars.index);
            }
        } else {
            throw new ParseException("Invalid start of Character: ", chars.index);
        }
    }

    public Token lexString() {
        if (match(RegexPattern.DOUBLE_QUOTE)) {
            while (peek(RegexPattern.STRING)) {
                if (peek(RegexPattern.BACKSLASH)) {
                    lexEscape();
                } else {
                    match(RegexPattern.STRING);
                }
            }
            if (match(RegexPattern.DOUBLE_QUOTE)) {
                return chars.emit(Token.Type.STRING);
            } else if (!match(RegexPattern.STRING)) {
                throw new ParseException("Unterminated string: ", chars.index);
            }
        } else {
            throw new ParseException("Invalid start of String: ", chars.index);
        }
        throw new RuntimeException("An unexpected error has occurred.");
    }

    public void lexEscape() {
        if (!match(RegexPattern.BACKSLASH, RegexPattern.ESCAPE_BODY)) {
            throw new ParseException("Invalid escape: ", chars.index + 2);
        }
    }

    public Token lexOperator() {
        if (!match(RegexPattern.OPERATOR, RegexPattern.EQUAL)) {
            match(RegexPattern.NONWHITESPACE);
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String ... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String ... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
