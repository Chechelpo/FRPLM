package io.github.chechelpo.frplm.utils.macros;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutletTest {

    @Nested
    class NormalizationTests {

        @Test
        void getNormalizedOutletAddsOutletPrefix() {
            assertEquals(
                    "outlet:content",
                    Outlet.getNormalizedOutlet("content")
            );
        }

        @Test
        void getNormalizedOutletPreservesExistingPrefix() {
            assertEquals(
                    "outlet:content",
                    Outlet.getNormalizedOutlet("outlet:content")
            );
        }

        @Test
        void getNormalizedOutletRemovesWhitespace() {
            assertEquals(
                    "outlet:mainContent",
                    Outlet.getNormalizedOutlet(" main Content ")
            );
        }

        @Test
        void getNormalizedOutletRemovesWhitespaceAroundPrefix() {
            assertEquals(
                    "outlet:mainContent",
                    Outlet.getNormalizedOutlet(" outlet : main Content ")
            );
        }

        @Test
        void getNormalizedOutletDoesNotDuplicatePrefix() {
            String normalized = Outlet.getNormalizedOutlet("outlet:main");

            assertEquals("outlet:main", normalized);
            assertFalse(normalized.startsWith("outlet:outlet:"));
        }

        @Test
        void getNormalizedOutletRejectsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> Outlet.getNormalizedOutlet(null)
            );
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructorCreatesOutletMacro() {
            Outlet outlet = new Outlet("content");

            assertEquals(
                    "{{outlet:content}}",
                    outlet.getName()
            );
        }

        @Test
        void constructorNormalizesWhitespace() {
            Outlet outlet = new Outlet(" main Content ");

            assertEquals(
                    "{{outlet:mainContent}}",
                    outlet.getName()
            );
        }

        @Test
        void constructorAcceptsExistingOutletPrefix() {
            Outlet outlet = new Outlet("outlet:content");

            assertEquals(
                    "{{outlet:content}}",
                    outlet.getName()
            );
        }

        @Test
        void constructorDoesNotDuplicateExistingPrefix() {
            Outlet outlet = new Outlet("outlet:content");

            assertNotEquals(
                    "{{outlet:outlet:content}}",
                    outlet.getName()
            );
        }

        @Test
        void constructorRejectsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> new Outlet(null)
            );
        }
    }

    @Nested
    class ReplacementTests {

        @Test
        void replaceAtReplacesOutletMacro() {
            Outlet outlet = new Outlet("content");

            String result = outlet.replaceAt(
                    "Before {{outlet:content}} after",
                    "injected"
            );

            assertEquals(
                    "Before injected after",
                    result
            );
        }

        @Test
        void replaceAtReplacesAllOutletOccurrences() {
            Outlet outlet = new Outlet("content");

            String result = outlet.replaceAt(
                    "{{outlet:content}} / {{outlet:content}}",
                    "value"
            );

            assertEquals("value / value", result);
        }

        @Test
        void replaceAtMatchesCaseInsensitively() {
            Outlet outlet = new Outlet("content");

            String result = outlet.replaceAt(
                    "{{OUTLET:CONTENT}}",
                    "value"
            );

            assertEquals("value", result);
        }

        @Test
        void replaceAtDoesNotReplaceOrdinaryMacroWithSameName() {
            Outlet outlet = new Outlet("content");

            String content = "{{content}}";

            assertEquals(
                    content,
                    outlet.replaceAt(content, "value")
            );
        }

        @Test
        void replaceAtDoesNotReplaceDifferentOutlet() {
            Outlet outlet = new Outlet("main");

            String content = "{{outlet:sidebar}}";

            assertEquals(
                    content,
                    outlet.replaceAt(content, "value")
            );
        }

        @Test
        void replaceAtHandlesRegexCharactersInOutletName() {
            Outlet outlet = new Outlet("main.content+");

            String result = outlet.replaceAt(
                    "{{outlet:main.content+}}",
                    "value"
            );

            assertEquals("value", result);
        }

        @Test
        void replaceAtHandlesSpecialReplacementCharacters() {
            Outlet outlet = new Outlet("path");
            String replacement = "$1\\temporary\\file";

            String result = outlet.replaceAt(
                    "{{outlet:path}}",
                    replacement
            );

            assertEquals(replacement, result);
        }
    }

    @Nested
    class EqualityTests {

        @Test
        void outletsWithEquivalentNamesAreEqual() {
            Outlet first = new Outlet("main content");
            Outlet second = new Outlet("outlet:mainContent");

            assertEquals(first, second);
        }

        @Test
        void equivalentOutletsHaveSameHashCode() {
            Outlet first = new Outlet("main content");
            Outlet second = new Outlet("outlet:mainContent");

            assertEquals(
                    first.hashCode(),
                    second.hashCode()
            );
        }

        @Test
        void outletsWithDifferentNamesAreNotEqual() {
            Outlet first = new Outlet("main");
            Outlet second = new Outlet("sidebar");

            assertNotEquals(first, second);
        }

        @Test
        void outletIsNotEqualToOrdinaryMacroWithDifferentNormalizedName() {
            Outlet outlet = new Outlet("content");
            Macro macro = new Macro("content");

            assertNotEquals(outlet, macro);
        }

        @Test
        void outletMayEqualEquivalentMacroDependingOnMacroEqualityContract() {
            Outlet outlet = new Outlet("content");
            Macro macro = new Macro("outlet:content");

            assertEquals(outlet, macro);
        }
    }
}