package io.github.chechelpo.frplm.utils.matching;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MacroTest {

    @Nested
    class NormalizationTests {

        @Test
        void getNormalizedRemovesWhitespace() {
            assertEquals(
                    "macroName",
                    Macro.getNormalized(" macro \t Name \n")
            );
        }

        @Test
        void asMacroAddsDelimiters() {
            assertEquals(
                    "{{macroName}}",
                    Macro.asMacro("macroName")
            );
        }

        @Test
        void asMacroRemovesWhitespaceFromName() {
            assertEquals(
                    "{{macroName}}",
                    Macro.asMacro(" macro Name ")
            );
        }

        @Test
        void asMacroDoesNotDuplicateExistingDelimiters() {
            assertEquals(
                    "{{macroName}}",
                    Macro.asMacro("{{ macroName }}")
            );
        }

        @Test
        void constructorNormalizesMacroName() {
            Macro macro = new Macro(" macro Name ");

            assertEquals("{{macroName}}", macro.getName());
        }

        @Test
        void constructorAcceptsAlreadyDelimitedMacro() {
            Macro macro = new Macro("{{ macroName }}");

            assertEquals("{{macroName}}", macro.getName());
        }

        @Test
        void constructorRejectsNullName() {
            assertThrows(
                    NullPointerException.class,
                    () -> new Macro(null)
            );
        }

        @Test
        void asMacroRejectsNullName() {
            assertThrows(
                    NullPointerException.class,
                    () -> Macro.asMacro(null)
            );
        }
    }

    @Nested
    class MacroDetectionTests {

        @Test
        void isMacroReturnsTrueForValidMacro() {
            assertTrue(Macro.isMacro("{{macroName}}"));
        }

        @Test
        void isMacroIgnoresWhitespace() {
            assertTrue(Macro.isMacro(" { { macro Name } } "));
        }

        @Test
        void isMacroReturnsFalseForPlainText() {
            assertFalse(Macro.isMacro("macroName"));
        }

        @Test
        void isMacroReturnsFalseWhenOnlyOpeningDelimiterExists() {
            assertFalse(Macro.isMacro("{{macroName"));
        }

        @Test
        void isMacroReturnsFalseWhenOnlyClosingDelimiterExists() {
            assertFalse(Macro.isMacro("macroName}}"));
        }

        @Test
        void isMacroReturnsFalseForNull() {
            assertFalse(Macro.isMacro(null));
        }

        @Test
        void isMacroReturnsFalseWhenMacroIsEmbeddedInOtherText() {
            assertFalse(Macro.isMacro("prefix{{macro}}suffix"));
        }
    }

    @Nested
    class PatternTests {

        @Test
        void asPatternMatchesMacro() {
            Macro macro = new Macro("macroName");
            Pattern pattern = macro.asPattern();

            assertTrue(pattern.matcher("{{macroName}}").matches());
        }

        @Test
        void asPatternIsCaseInsensitive() {
            Macro macro = new Macro("macroName");

            assertTrue(
                    macro.asPattern()
                            .matcher("{{MACRONAME}}")
                            .matches()
            );
        }

        @Test
        void asPatternTreatsRegexCharactersAsLiteralText() {
            Macro macro = new Macro("a+b.c?[x]");

            assertTrue(
                    macro.asPattern()
                            .matcher("{{a+b.c?[x]}}")
                            .matches()
            );
        }

        @Test
        void asPatternDoesNotInterpretPlusAsRegexQuantifier() {
            Macro macro = new Macro("value+");

            assertTrue(
                    macro.asPattern()
                            .matcher("{{value+}}")
                            .matches()
            );

            assertFalse(
                    macro.asPattern()
                            .matcher("{{valueee}}")
                            .matches()
            );
        }

        @Test
        void asPatternFindsMacroInsideContent() {
            Macro macro = new Macro("name");

            assertTrue(
                    macro.asPattern()
                            .matcher("Hello {{name}}!")
                            .find()
            );
        }
    }

    @Nested
    class ReplacementTests {

        @Test
        void replaceAtReplacesMacro() {
            Macro macro = new Macro("toReplaceMacro");

            String result = macro.replaceAt(
                    "Before {{toReplaceMacro}} after",
                    "injected value"
            ).newContent();

            assertEquals(
                    "Before injected value after",
                    result
            );
        }

        @Test
        void replaceAtReplacesAllOccurrences() {
            Macro macro = new Macro("value");

            String result = macro.replaceAt(
                    "{{value}} + {{value}} = {{value}}",
                    "X"
            ).newContent();

            assertEquals("X + X = X", result);
        }

        @Test
        void replaceAtIsCaseInsensitive() {
            Macro macro = new Macro("username");

            String result = macro.replaceAt(
                    "{{USERNAME}} / {{UserName}} / {{username}}",
                    "Marco"
            ).newContent();

            assertEquals("Marco / Marco / Marco", result);
        }

        @Test
        void replaceAtHandlesRegexCharactersInMacroName() {
            Macro macro = new Macro("field.value+");

            String result = macro.replaceAt(
                    "Result: {{field.value+}}",
                    "42"
            ).newContent();

            assertEquals("Result: 42", result);
        }

        @Test
        void replaceAtHandlesDollarSignInReplacement() {
            Macro macro = new Macro("price");

            String result = macro.replaceAt(
                    "Price: {{price}}",
                    "$100"
            ).newContent();

            assertEquals("Price: $100", result);
        }

        @Test
        void replaceAtHandlesBackslashesInReplacement() {
            Macro macro = new Macro("path");

            String replacement = "C:\\users\\marco";

            String result = macro.replaceAt(
                    "Path: {{path}}",
                    replacement
            ).newContent();

            assertEquals(
                    "Path: C:\\users\\marco",
                    result
            );
        }

        @Test
        void replaceAtHandlesGroupLikeReplacementText() {
            Macro macro = new Macro("value");

            String replacement = "$1\\temporary\\file";

            String result = macro.replaceAt(
                    "{{value}}",
                    replacement
            ).newContent();

            assertEquals(replacement, result);
        }

        @Test
        void replaceAtReturnsContentUnchangedWhenMacroIsAbsent() {
            Macro macro = new Macro("missing");

            String content = "No macro exists here";

            assertEquals(
                    content,
                    macro.replaceAt(content, "replacement").newContent()
            );
        }

        @Test
        void replaceAtDoesNotReplacePartialMacroName() {
            Macro macro = new Macro("user");

            String content = "{{username}}";

            assertEquals(
                    content,
                    macro.replaceAt(content, "Marco").newContent()
            );
        }

        @Test
        void replaceAtReturnsContentUnchangedForNullInjection() {
            Macro macro = new Macro("value");
            String content = "{{value}}";

            assertEquals(
                    content,
                    macro.replaceAt(content, null).newContent()
            );
        }

        @Test
        void replaceAtReturnsContentUnchangedForEmptyInjection() {
            Macro macro = new Macro("value");
            String content = "{{value}}";

            assertEquals(
                    content,
                    macro.replaceAt(content, "").newContent()
            );
        }

        @Test
        void replaceAtReturnsContentUnchangedForBlankInjection() {
            Macro macro = new Macro("value");
            String content = "{{value}}";

            assertEquals(
                    content,
                    macro.replaceAt(content, "   \t").newContent()
            );
        }

        @Test
        void replaceAtRejectsNullContent() {
            Macro macro = new Macro("value");

            assertThrows(
                    NullPointerException.class,
                    () -> macro.replaceAt(null, "replacement")
            );
        }
    }

    @Nested
    class StripUnresolvedMacrosTests {

        @Test
        void stripUnresolvedMacrosRemovesInlineMacro() {
            String result = Macro.stripUnresolvedMacros(
                    "Hello {{username}}!"
            );

            assertEquals("Hello !", result);
        }

        @Test
        void stripUnresolvedMacrosRemovesMultipleInlineMacros() {
            String result = Macro.stripUnresolvedMacros(
                    "{{firstName}} {{lastName}}: {{message}}"
            );

            assertEquals(" : ", result);
        }

        @Test
        void stripUnresolvedMacrosRemovesStandaloneMacroLine() {
            String content = """
                    first line
                    {{unresolved}}
                    second line
                    """;

            String expected = """
                    first line
                    second line
                    """;

            assertEquals(
                    expected,
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosRemovesIndentedMacroLine() {
            String content =
                    "first line\n" +
                            "    {{unresolved}}    \n" +
                            "second line";

            assertEquals(
                    "first line\nsecond line",
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosSupportsWindowsLineEndings() {
            String content =
                    "first line\r\n" +
                            "\t{{unresolved}}\r\n" +
                            "second line";

            assertEquals(
                    "first line\r\nsecond line",
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosRemovesMacroAtEndWithoutLineBreak() {
            String content =
                    "first line\n" +
                            "{{unresolved}}";

            assertEquals(
                    "first line\n",
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosPreservesOrdinaryContent() {
            String content = "There are no unresolved macros.";

            assertEquals(
                    content,
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosPreservesEmptyMacro() {
            String content = "Before {{}} after";

            assertEquals(
                    content,
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosPreservesMalformedOpeningMacro() {
            String content = "Before {{missing after";

            assertEquals(
                    content,
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosPreservesMalformedClosingMacro() {
            String content = "Before missing}} after";

            assertEquals(
                    content,
                    Macro.stripUnresolvedMacros(content)
            );
        }

        @Test
        void stripUnresolvedMacrosRejectsNullContent() {
            assertThrows(
                    NullPointerException.class,
                    () -> Macro.stripUnresolvedMacros(null)
            );
        }
    }

    @Nested
    class EqualityTests {

        @Test
        void equalMacrosHaveSameNormalizedName() {
            Macro first = new Macro("macro Name");
            Macro second = new Macro("macroName");

            assertEquals(first, second);
        }

        @Test
        void equalMacrosHaveSameHashCode() {
            Macro first = new Macro("macro Name");
            Macro second = new Macro("macroName");

            assertEquals(
                    first.hashCode(),
                    second.hashCode()
            );
        }

        @Test
        void macroEqualsItself() {
            Macro macro = new Macro("name");

            assertEquals(macro, macro);
        }

        @Test
        void macroDoesNotEqualNull() {
            Macro macro = new Macro("name");

            assertNotEquals(null, macro);
        }

        @Test
        void macroDoesNotEqualDifferentType() {
            Macro macro = new Macro("name");

            assertNotEquals("{{name}}", macro);
        }

        @Test
        void macrosWithDifferentNamesAreNotEqual() {
            Macro first = new Macro("first");
            Macro second = new Macro("second");

            assertNotEquals(first, second);
        }
    }
}