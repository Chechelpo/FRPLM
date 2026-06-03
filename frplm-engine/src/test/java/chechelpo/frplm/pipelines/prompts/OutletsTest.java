package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class OutletsTest {
        private static final Method INJECT_ENTRIES =
                method("injectEntries", String.class, List.class, Map.class);

        private static final Method ABSOLUTE_OFFSET_OF =
                method("absoluteOffsetOf", String.class, OutletInjection.DetectedOutlet.class);

        private static final Method OUTLET_MARKER_END =
                method("outletMarkerEnd", String.class, int.class);

        private static final Method RENDER_ENTRIES =
                method("renderEntries", List.class);

        private static final Method GET_OUTLET_LOCATION_BY_NAME =
                method("getOutletLocation", String.class, String.class);

        private static final Method GET_OUTLET_LOCATION_BY_PATTERN =
                method("getOutletLocation", String.class, Pattern.class);

        // -----------------------------------------------------------------------------------------------------------------
        // Public detection API
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("getDetectedOutlets")
        final class GetDetectedOutletsTests {

            @Test
            void detectsSingleOutletOnSingleLine() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(outlet(10, "memory")),
                                "A {{memory}} B"
                        );

                assertEquals(1, result.size());
                assertDetected(result.getFirst(), 10, 0, 2);
            }

            @Test
            void detectionIsCaseInsensitive() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(outlet(10, "memory")),
                                "A {{MeMoRy}} B"
                        );

                assertEquals(1, result.size());
                assertDetected(result.getFirst(), 10, 0, 2);
            }

            @Test
            void outletNameIsRegexQuoted() {
                String outletName = "a.b+?[]()^$|";

                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(outlet(7, outletName)),
                                "X {{" + outletName + "}} Y"
                        );

                assertEquals(1, result.size());
                assertDetected(result.getFirst(), 7, 0, 2);
            }

            @Test
            void ignoresBlankOutletNames() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, ""),
                                        outlet(2, " "),
                                        outlet(3, "\t"),
                                        outlet(4, "\n")
                                ),
                                "{{}} {{ }} {{\t}}"
                        );

                assertTrue(result.isEmpty());
            }

            @Test
            void doesNotMatchPartialOrMalformedMarkers() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(outlet(1, "memory")),
                                "{{memory-extra}} {memory} {{ memory }} [[memory]]"
                        );

                assertTrue(result.isEmpty());
            }

            @Test
            void returnsOnlyFirstOccurrenceForEachOutletDefinition() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(outlet(1, "key")),
                                "xx {{key}}\nyy {{key}}"
                        );

                assertEquals(1, result.size());
                assertDetected(result.getFirst(), 1, 0, 3);
            }

            @Test
            void preservesOutletArrayOrderRatherThanTextualOrder() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(2, "b"),
                                        outlet(1, "a")
                                ),
                                "{{a}} {{b}}"
                        );

                assertEquals(2, result.size());
                assertDetected(result.get(0), 2, 0, 6);
                assertDetected(result.get(1), 1, 0, 0);
            }

            @Test
            void duplicateOutletNamesProduceDuplicateDetectionsWithDifferentIds() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, "same"),
                                        outlet(2, "same")
                                ),
                                "abc {{same}}"
                        );

                assertEquals(2, result.size());
                assertDetected(result.get(0), 1, 0, 4);
                assertDetected(result.get(1), 2, 0, 4);
            }

            @Test
            void detectsAcrossLfCrLfAndCrSeparatedSegments() {
                String text = "first\nxx {{B}}\r\nyyy {{C}}\rz {{D}}";

                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, "B"),
                                        outlet(2, "C"),
                                        outlet(3, "D")
                                ),
                                text
                        );

                assertEquals(3, result.size());
                assertDetected(result.get(0), 1, 1, 3);
                assertDetected(result.get(1), 2, 2, 4);
                assertDetected(result.get(2), 3, 3, 2);
            }

            @Test
            void detectsOutletAfterLeadingEmptyLine() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(outlet(1, "key")),
                                "\n{{key}}"
                        );

                assertEquals(1, result.size());
                assertDetected(result.getFirst(), 1, 1, 0);
            }

            @Test
            void returnsEmptyWhenNoOutletMatches() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, "alpha"),
                                        outlet(2, "beta")
                                ),
                                "plain text without markers"
                        );

                assertTrue(result.isEmpty());
            }

            @Test
            void emptyOutletArrayAlwaysReturnsEmptyResult() {
                List<IntObjectPair<OutletInjection.DetectedOutlet>> result =
                        OutletDetection.getDetectedOutlets(
                                outlets(),
                                "{{alpha}}"
                        );

                assertTrue(result.isEmpty());
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // getReadyToInsert
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("getReadyToInsert")
        final class GetReadyToInsertTests {

            @Test
            void keepsOnlySectionsWithDetectedOutlets() {
                PromptSectionRecord first = section("no markers here");
                PromptSectionRecord second = section("has {{alpha}}");
                PromptSectionRecord third = section("also has {{beta}}");

                List<OutletInjection.OutletsOfSections> result =
                        OutletDetection.getReadyToInsert(
                                outlets(
                                        outlet(1, "alpha"),
                                        outlet(2, "beta")
                                ),
                                List.of(first, second, third)
                        );

                assertEquals(2, result.size());

                assertSame(second, result.get(0).section());
                assertEquals(1, result.get(0).outlets().size());
                assertDetected(result.get(0).outlets().getFirst(), 1, 0, 4);

                assertSame(third, result.get(1).section());
                assertEquals(1, result.get(1).outlets().size());
                assertDetected(result.get(1).outlets().getFirst(), 2, 0, 9);
            }

            @Test
            void returnsEmptyWhenNoSectionContainsOutlet() {
                PromptSectionRecord first = section("plain one");
                PromptSectionRecord second = section("plain two");

                List<OutletInjection.OutletsOfSections> result =
                        OutletDetection.getReadyToInsert(
                                outlets(outlet(1, "alpha")),
                                List.of(first, second)
                        );

                assertTrue(result.isEmpty());
            }

            @Test
            void returnsEmptyForEmptySectionList() {
                List<OutletInjection.OutletsOfSections> result =
                        OutletDetection.getReadyToInsert(
                                outlets(outlet(1, "alpha")),
                                List.of()
                        );

                assertTrue(result.isEmpty());
            }

            @Test
            void wrapperOverloadReturnsEmptyWhenOutletListIsEmpty() {
                PromptSectionRecord section = section("{{alpha}}");

                List<OutletInjection.OutletsOfSections> result =
                        OutletDetection.getReadyToInsert(section, List.of());

                assertTrue(result.isEmpty());
            }

            @Test
            void wrapperOverloadPreservesSectionAndOutletListIdentity() {
                PromptSectionRecord section = section("{{alpha}}");
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detected =
                        new ArrayList<>(List.of(detected(1, 0, 0)));

                List<OutletInjection.OutletsOfSections> result =
                        OutletDetection.getReadyToInsert(section, detected);

                assertEquals(1, result.size());
                assertSame(section, result.getFirst().section());
                assertSame(detected, result.getFirst().outlets());
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Private getOutletLocation overloads
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("private getOutletLocation")
        final class GetOutletLocationTests {

            @Test
            void byNameReturnsEmptyForBlankOutletName() {
                assertTrue(getOutletLocation("{{x}}", "").isEmpty());
                assertTrue(getOutletLocation("{{x}}", " ").isEmpty());
                assertTrue(getOutletLocation("{{x}}", "\t").isEmpty());
            }

            @Test
            void byNameFindsOutletOnFirstSegment() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("abc {{x}}", "x");

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(0, 4)), result);
            }

            @Test
            void byNameFindsOutletAfterLf() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("abc\nxx {{x}}", "x");

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(1, 3)), result);
            }

            @Test
            void byNameFindsOutletAfterCrLf() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("abc\r\nxx {{x}}", "x");

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(1, 3)), result);
            }

            @Test
            void byNameFindsOutletAfterCr() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("abc\rxx {{x}}", "x");

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(1, 3)), result);
            }

            @Test
            void byNameReturnsFirstMatchOnly() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("{{x}}\n{{x}}\n{{x}}", "x");

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(0, 0)), result);
            }

            @Test
            void byPatternFindsRegexInsideSingleSegment() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("abc\nxx 123 yy", Pattern.compile("\\d+"));

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(1, 3)), result);
            }

            @Test
            void byPatternDoesNotMatchAcrossSegmentBoundary() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("a\nb", Pattern.compile("a\\R?b"));

                assertTrue(result.isEmpty());
            }

            @Test
            void byPatternReturnsEmptyOnEmptyTextWhenPatternDoesNotMatchEmptyString() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("", Pattern.compile("x"));

                assertTrue(result.isEmpty());
            }

            @Test
            void byPatternCanMatchEmptyTextIfPatternAllowsEmptyMatch() {
                Optional<OutletInjection.DetectedOutlet> result =
                        getOutletLocation("", Pattern.compile(""));

                assertEquals(Optional.of(new OutletInjection.DetectedOutlet(0, 0)), result);
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // absoluteOffsetOf
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("private absoluteOffsetOf")
        final class AbsoluteOffsetOfTests {

            @Test
            void computesOffsetOnFirstSegment() {
                assertEquals(
                        4,
                        absoluteOffsetOf("abc {{x}}", new OutletInjection.DetectedOutlet(0, 4))
                );
            }

            @Test
            void computesOffsetAcrossMixedLineSeparators() {
                String text = "ab\ncde\r\nfg\rhi";

                assertAll(
                        () -> assertEquals(0, absoluteOffsetOf(text, new OutletInjection.DetectedOutlet(0, 0))),
                        () -> assertEquals(5, absoluteOffsetOf(text, new OutletInjection.DetectedOutlet(1, 2))),
                        () -> assertEquals(9, absoluteOffsetOf(text, new OutletInjection.DetectedOutlet(2, 1))),
                        () -> assertEquals(12, absoluteOffsetOf(text, new OutletInjection.DetectedOutlet(3, 1)))
                );
            }

            @Test
            void returnsTextLengthWhenRequestedSegmentDoesNotExist() {
                String text = "ab\ncd";

                assertEquals(
                        text.length(),
                        absoluteOffsetOf(text, new OutletInjection.DetectedOutlet(99, 0))
                );
            }

            @Test
            void clampsOffsetToTextLengthWhenCharOffsetIsTooLarge() {
                String text = "abc";

                assertEquals(
                        text.length(),
                        absoluteOffsetOf(text, new OutletInjection.DetectedOutlet(0, 999))
                );
            }

            @Test
            void detectedLocationRoundTripsToMarkerStart() {
                String text = "first\nxx {{alpha}}\r\ny {{beta}}\rz {{gamma}}";

                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, "alpha"),
                                        outlet(2, "beta"),
                                        outlet(3, "gamma")
                                ),
                                text
                        );

                for (IntObjectPair<OutletInjection.DetectedOutlet> detection : detections) {
                    int absoluteOffset = absoluteOffsetOf(text, detection.second());
                    assertTrue(
                            text.startsWith("{{", absoluteOffset),
                            "Detected location must resolve to an absolute marker start"
                    );
                }
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // outletMarkerEnd
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("private outletMarkerEnd")
        final class OutletMarkerEndTests {

            @Test
            void returnsEndExclusiveForSimpleMarker() {
                assertEquals(5, outletMarkerEnd("{{a}}", 0));
                assertEquals(7, outletMarkerEnd("{{abc}}", 0));
            }

            @Test
            void returnsEndExclusiveForPrefixedMarker() {
                assertEquals(10, outletMarkerEnd("xx {{abc}} yy", 3));
            }

            @Test
            void supportsEmptyMarker() {
                assertEquals(4, outletMarkerEnd("{{}}", 0));
            }

            @Test
            void stopsAtFirstClosingDelimiter() {
                assertEquals(5, outletMarkerEnd("{{a}} {{b}}", 0));
            }

            @Test
            void returnsStartWhenMarkerStartIsNegative() {
                assertEquals(-1, outletMarkerEnd("{{a}}", -1));
            }

            @Test
            void returnsStartWhenMarkerStartIsPastEnd() {
                assertEquals(99, outletMarkerEnd("{{a}}", 99));
            }

            @Test
            void returnsStartWhenMarkerStartIsLastCharacter() {
                assertEquals(3, outletMarkerEnd("abc{", 3));
            }

            @Test
            void returnsStartWhenStartDoesNotPointToDoubleOpeningBrace() {
                assertEquals(1, outletMarkerEnd("{{a}}", 1));
                assertEquals(0, outletMarkerEnd("{a}}", 0));
                assertEquals(1, outletMarkerEnd("xx{{a}}", 2 - 1));
            }

            @Test
            void returnsStartWhenClosingDelimiterIsMissing() {
                assertEquals(0, outletMarkerEnd("{{abc", 0));
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // renderEntries
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("private renderEntries")
        final class RenderEntriesTests {

            @Test
            void emptyListRendersAsEmptyString() {
                assertEquals("", renderEntries(List.of()));
            }

            @Test
            void singleEntryRendersAsItsContent() {
                assertEquals("alpha", renderEntries(List.of(entry("alpha"))));
            }

            @Test
            void multipleEntriesAreJoinedWithNewline() {
                assertEquals(
                        "alpha\nbeta\ngamma",
                        renderEntries(List.of(
                                entry("alpha"),
                                entry("beta"),
                                entry("gamma")
                        ))
                );
            }

            @Test
            void nullAndBlankContentsAreIgnored() {
                assertEquals(
                        "alpha\nbeta",
                        renderEntries(List.of(
                                entry(null),
                                entry(""),
                                entry(" "),
                                entry("\t"),
                                entry("\n"),
                                entry("alpha"),
                                entry("beta")
                        ))
                );
            }

            @Test
            void nonBlankContentIsPreservedExactly() {
                assertEquals(
                        "  alpha  \n\tbeta\t",
                        renderEntries(List.of(
                                entry("  alpha  "),
                                entry("\tbeta\t")
                        ))
                );
            }

            @Test
            void allNullOrBlankEntriesRenderAsEmptyString() {
                assertEquals(
                        "",
                        renderEntries(List.of(
                                entry(null),
                                entry(""),
                                entry("   "),
                                entry("\r\n")
                        ))
                );
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // injectEntries
        // -----------------------------------------------------------------------------------------------------------------

        @Nested
        @DisplayName("private injectEntries")
        final class InjectEntriesTests {

            @Test
            void returnsOriginalContentWhenDetectedOutletsAreEmpty() {
                String content = "x {{A}} y";

                String result = injectEntries(
                        content,
                        List.of(),
                        Map.of(1, List.of(entry("alpha")))
                );

                assertSame(content, result);
            }

            @Test
            void returnsOriginalContentWhenInjectionMapIsEmpty() {
                String content = "x {{A}} y";

                String result = injectEntries(
                        content,
                        List.of(detected(1, 0, 2)),
                        Map.of()
                );

                assertSame(content, result);
            }

            @Test
            void leavesMarkerUnchangedWhenOutletIdHasNoEntries() {
                String content = "x {{A}} y";

                String result = injectEntries(
                        content,
                        List.of(detected(1, 0, 2)),
                        Map.of(2, List.of(entry("wrong")))
                );

                assertEquals(content, result);
            }

            @Test
            void replacesSingleOutletMarker() {
                String content = "pre {{A}} post";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(outlets(outlet(1, "A")), content);

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(1, List.of(entry("alpha")))
                );

                assertEquals("pre alpha post", result);
            }

            @Test
            void replacesCaseInsensitiveDetectedMarker() {
                String content = "pre {{LoRe}} post";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(outlets(outlet(1, "lore")), content);

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(1, List.of(entry("payload")))
                );

                assertEquals("pre payload post", result);
            }

            @Test
            void multipleEntriesForOneOutletAreJoinedWithNewlines() {
                String content = "pre {{A}} post";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(outlets(outlet(1, "A")), content);

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(1, List.of(
                                entry("one"),
                                entry("two"),
                                entry("three")
                        ))
                );

                assertEquals("pre one\ntwo\nthree post", result);
            }

            @Test
            void blankAndNullEntriesAreSkippedDuringInjection() {
                String content = "pre {{A}} post";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(outlets(outlet(1, "A")), content);

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(1, List.of(
                                entry(null),
                                entry(""),
                                entry(" "),
                                entry("payload"),
                                entry("  kept  ")
                        ))
                );

                assertEquals("pre payload\n  kept   post", result);
            }

            @Test
            void allBlankEntriesRemoveTheMarker() {
                String content = "x{{A}}y";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(outlets(outlet(1, "A")), content);

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(1, List.of(
                                entry(null),
                                entry(""),
                                entry("   ")
                        ))
                );

                assertEquals("xy", result);
            }

            @Test
            void replacesSeveralOutletsWithoutOffsetShiftBugs() {
                String content = "{{A}}-{{B}}-{{C}}";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, "A"),
                                        outlet(2, "B"),
                                        outlet(3, "C")
                                ),
                                content
                        );

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(
                                1, List.of(entry("123456")),
                                2, List.of(entry("B")),
                                3, List.of(entry(""))
                        )
                );

                assertEquals("123456-B-", result);
            }

            @Test
            void preservesOriginalLineSeparatorsAroundInjectedMarkers() {
                String content = "first\nx {{A}}\r\ny {{B}}\rz {{C}}";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(
                                outlets(
                                        outlet(1, "A"),
                                        outlet(2, "B"),
                                        outlet(3, "C")
                                ),
                                content
                        );

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(
                                1, List.of(entry("alpha")),
                                2, List.of(entry("beta")),
                                3, List.of(entry("gamma"))
                        )
                );

                assertEquals("first\nx alpha\r\ny beta\rz gamma", result);
            }

            @Test
            void replacementResultDoesNotDependOnDetectionOrder() {
                String content = "{{A}}{{B}}";

                List<IntObjectPair<OutletInjection.DetectedOutlet>> orderAThenB =
                        List.of(
                                detected(1, 0, 0),
                                detected(2, 0, 5)
                        );

                List<IntObjectPair<OutletInjection.DetectedOutlet>> orderBThenA =
                        List.of(
                                detected(2, 0, 5),
                                detected(1, 0, 0)
                        );

                Map<Integer, List<EntryRecord>> payloads =
                        Map.of(
                                1, List.of(entry("longer-alpha")),
                                2, List.of(entry("b"))
                        );

                assertEquals(
                        "longer-alphab",
                        injectEntries(content, orderAThenB, payloads)
                );

                assertEquals(
                        "longer-alphab",
                        injectEntries(content, orderBThenA, payloads)
                );
            }

            @Test
            void supportsMultipleDetectionsForSameOutletIdIfProvidedByCaller() {
                String content = "{{A}} {{A}}";

                String result = injectEntries(
                        content,
                        List.of(
                                detected(1, 0, 0),
                                detected(1, 0, 6)
                        ),
                        Map.of(1, List.of(entry("x")))
                );

                assertEquals("x x", result);
            }

            @Test
            void publicDetectionOnlyInjectsFirstOccurrenceOfSameOutletName() {
                String content = "{{A}} {{A}}";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        OutletDetection.getDetectedOutlets(outlets(outlet(1, "A")), content);

                String result = injectEntries(
                        content,
                        detections,
                        Map.of(1, List.of(entry("x")))
                );

                assertEquals("x {{A}}", result);
            }

            @Test
            void doesNotMutateDetectionListOrInjectionMap() {
                String content = "{{A}}";
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detections =
                        new ArrayList<>(List.of(detected(1, 0, 0)));

                List<EntryRecord> entries = new ArrayList<>(List.of(entry("alpha")));
                Map<Integer, List<EntryRecord>> map = new HashMap<>();
                map.put(1, entries);

                String result = injectEntries(content, detections, map);

                assertEquals("alpha", result);
                assertEquals(1, detections.size());
                assertDetected(detections.getFirst(), 1, 0, 0);
                assertSame(entries, map.get(1));
                assertEquals(1, map.size());
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Reflection wrappers
        // -----------------------------------------------------------------------------------------------------------------

        @SuppressWarnings("unchecked")
        private static String injectEntries(
                String content,
                List<IntObjectPair<OutletInjection.DetectedOutlet>> detectedOutlets,
                Map<Integer, List<EntryRecord>> toInject
        ) {
            return invoke(INJECT_ENTRIES, content, detectedOutlets, toInject);
        }

        private static int absoluteOffsetOf(
                String text,
                OutletInjection.DetectedOutlet location
        ) {
            return invoke(ABSOLUTE_OFFSET_OF, text, location);
        }

        private static int outletMarkerEnd(String text, int markerStart) {
            return invoke(OUTLET_MARKER_END, text, markerStart);
        }

        private static String renderEntries(List<EntryRecord> entries) {
            return invoke(RENDER_ENTRIES, entries);
        }

        @SuppressWarnings("unchecked")
        private static Optional<OutletInjection.DetectedOutlet> getOutletLocation(
                String text,
                String outletName
        ) {
            return invoke(GET_OUTLET_LOCATION_BY_NAME, text, outletName);
        }

        @SuppressWarnings("unchecked")
        private static Optional<OutletInjection.DetectedOutlet> getOutletLocation(
                String text,
                Pattern pattern
        ) {
            return invoke(GET_OUTLET_LOCATION_BY_PATTERN, text, pattern);
        }

        private static Method method(String name, Class<?>... parameterTypes) {
            try {
                Method method = OutletInjection.class.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Missing method: " + name, exception);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T invoke(Method method, Object... arguments) {
            try {
                return (T) method.invoke(null, arguments);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();

                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                if (cause instanceof Error error) {
                    throw error;
                }

                throw new AssertionError("Unexpected checked exception from reflected method", cause);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Reflection failed", exception);
            }
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Test fixtures
        // -----------------------------------------------------------------------------------------------------------------

        private static EntryRecord entry(String content) {
            EntryRecord record = new EntryRecord();
            record.setContent(content);
            return record;
        }

        private static PromptSectionRecord section(String content) {
            PromptSectionRecord record = new PromptSectionRecord();
            record.setContent(content);
            return record;
        }

        private static IntObjectPair<String> outlet(int id, String name) {
            return new IntObjectImmutablePair<>(id, name);
        }

        private static IntObjectPair<OutletInjection.DetectedOutlet> detected(
                int outletId,
                int segmentIndex,
                int charOffset
        ) {
            return new IntObjectImmutablePair<>(
                    outletId,
                    new OutletInjection.DetectedOutlet(segmentIndex, charOffset)
            );
        }

        @SafeVarargs
        private static IntObjectPair<String>[] outlets(IntObjectPair<String>... outlets) {
            return outlets;
        }

        private static void assertDetected(
                IntObjectPair<OutletInjection.DetectedOutlet> actual,
                int expectedOutletId,
                int expectedSegmentIndex,
                int expectedCharOffset
        ) {
            assertEquals(expectedOutletId, actual.firstInt());
            assertEquals(
                    new OutletInjection.DetectedOutlet(expectedSegmentIndex, expectedCharOffset),
                    actual.second()
            );
        }

}