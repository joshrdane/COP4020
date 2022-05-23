package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Domain with hyphens", "local-part@email-domain.tld", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Missing local-part", "@gmail.com", false),
                Arguments.of("Local-part single period", ".@gmail.com", true),
                Arguments.of("Local-part single hyphen", "-@gmail.com", true),
                Arguments.of("Local-part Single alphabetic", "a@gmail.com", true),
                Arguments.of("Local-part Single numeric", "1@gmail.com", true),
                Arguments.of("Domain with hyphens", "local-part@email-domain.tld", true),
                Arguments.of("Domain with underscore", "local-part@email-domain.tld", true),
                Arguments.of("Top level domain greater than 3 characters", "local-part@email-domain.email", false),
                Arguments.of("Top level domain less than 2 characters", "local-part@email-domain.x", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas? -- very clever lol
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                Arguments.of("8 Characters", "a2c4e6g8", false),
                Arguments.of("9 Characters", "a2c4e6g8i", false),
                Arguments.of("10 Characters", "0123456789", true),
                Arguments.of("11 Characters", "0123456789A", false),
                Arguments.of("12 Characters", "0123456789AB", true),
                Arguments.of("18 Characters", "0123456789ABCDEFGH", true),
                Arguments.of("19 Characters", "0123456789ABCDEFGHI", false),
                Arguments.of("20 Characters", "0123456789ABCDEFGHIJ", true),
                Arguments.of("21 Characters", "0123456789ABCDEFGHIJK", false),
                Arguments.of("20 Characters", "0123456789ABCDEFGHIJKL", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("No Elements", "[]", true),
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements no spacing", "[1,2,3]", true),
                Arguments.of("Multiple Elements with spacing", "[1, 2, 3]", true),
                Arguments.of("Multiple Elements mixed spacing", "[1,2, 3]", true),
                Arguments.of("Trailing Comma", "[1,2,3,]", false),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Left Bracket", "1,2,3]", false),
                Arguments.of("Missing Right Bracket", "[1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                Arguments.of("Missing Single Comma", "[1 2, 3]", false),
                Arguments.of("Single Non-Integer Element", "[a]", false),
                Arguments.of("Non-Integer Element", "[1, a, 3]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("Single digit", "1", true),
                Arguments.of("Multiple digit with decimal", "123.456", true),
                Arguments.of("Negative decimal with trailing zero", "-1.0", true),
                Arguments.of("Positive single digit", "+1", true),
                Arguments.of("Positive multiple digit with decimal", "+123.456", true),
                Arguments.of("Positive single digit with trailing decimal", "+1.", false),
                Arguments.of("Single digit with trailing radix", "1.", false),
                Arguments.of("Single digit with leading radix", ".5", false),
                Arguments.of("Negative single digit with leading radix", "-.5", false),
                Arguments.of("Single radix", ".", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\"", true),
                Arguments.of("Numeric", "\"0123456789\"", true),
                Arguments.of("Symbolic", "\"!@#$%^&*()-=[]\\\\{}|;:'<>?/\"", true),
                Arguments.of("Alphanumeric", "\"a1b2c3\"", true),
                Arguments.of("Alphanumeric and symbolic", "\"a1b2c3???\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Single quote", "\"", false),
                Arguments.of("Missing beginning quote", "abc\"", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Escaped quotation resulting in unterminated string", "\"\\\"", false),
                Arguments.of("Escaped quotation", "\"\\\"\"", true)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
