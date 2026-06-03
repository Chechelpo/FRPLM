package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static chechelpo.frplm.pipelines.prompts.PromptEntryPoint.PROMPT_LOGGER;

public final class OutletDetection {
    private OutletDetection() {}

    /**
     * Detects outlets in a batch of messages.
     *
     * @param outlets  outletID -> outlet name
     * @param sections sections to scan
     * @return messages with their outlets (outlet list may be empty)
     */
    @Contract(pure = true)
    public static @NotNull List<OutletInjection.OutletsOfSections> getReadyToInsert(
            IntObjectPair<String> @NotNull [] outlets,
            @NotNull List<PromptSectionRecord> sections
    ) {
        List<OutletInjection.OutletsOfSections> readyToInsert = new ArrayList<>();

        for (PromptSectionRecord section : sections) {
            List<IntObjectPair<OutletInjection.DetectedOutlet>> detectedOutlets =
                    getDetectedOutlets(outlets, section.getContent());
            PROMPT_LOGGER.trace("SECTION: {} \n Detected: {}", section.getName(), detectedOutlets);
            readyToInsert.add(new OutletInjection.OutletsOfSections(section, detectedOutlets));
        }

        return readyToInsert;
    }

    /**
     * Wraps one an already-scanned section.
     * <p>
     * Useful if outlet detection was performed elsewhere.
     */
    @Contract(pure = true)
    public static @NotNull List<OutletInjection.OutletsOfSections> getReadyToInsert(
            @NotNull PromptSectionRecord section,
            @NotNull List<IntObjectPair<OutletInjection.DetectedOutlet>> outlets
    ) {
        List<OutletInjection.OutletsOfSections> readyToInsert = new ArrayList<>();

        if (!outlets.isEmpty()) {
            readyToInsert.add(new OutletInjection.OutletsOfSections(section, outlets));
        }

        return readyToInsert;
    }

    @Contract(pure = true)
    public static @NotNull List<IntObjectPair<OutletInjection.DetectedOutlet>> getDetectedOutlets(
            IntObjectPair<String> @NotNull [] outlets,
            @NotNull String message
    ) {
        List<IntObjectPair<OutletInjection.DetectedOutlet>> detectedOutlets = new ArrayList<>();

        for (IntObjectPair<String> outlet : outlets) {
            int outletID = outlet.firstInt();
            String outletName = outlet.second();

            getOutletLocation(message, outletName)
                    .ifPresent(location -> detectedOutlets.add(
                            new IntObjectImmutablePair<>(outletID, location)
                    ));
        }

        return detectedOutlets;
    }

    @Contract(pure = true)
    private static @NotNull Optional<OutletInjection.DetectedOutlet> getOutletLocation(
            @NotNull String text,
            @NotNull String outletName
    ) {
        if (outletName.isBlank()) {
            return Optional.empty();
        }

        Pattern pattern = StandardOutlet.asPattern(outletName);

        return getOutletLocation(text, pattern);
    }

    @Contract(pure = true)
    private static @NotNull Optional<OutletInjection.DetectedOutlet> getOutletLocation(
            @NotNull String text,
            @NotNull Pattern pattern
    ) {
        int segmentIndex = 0;
        int segmentStart = 0;

        while (segmentStart <= text.length()) {
            int segmentEnd = segmentStart;

            while (
                    segmentEnd < text.length()
                            && text.charAt(segmentEnd) != '\n'
                            && text.charAt(segmentEnd) != '\r'
            ) {
                segmentEnd++;
            }

            Matcher matcher = pattern.matcher(text);
            matcher.region(segmentStart, segmentEnd);

            if (matcher.find()) {
                return Optional.of(new OutletInjection.DetectedOutlet(
                        segmentIndex,
                        matcher.start() - segmentStart
                ));
            }

            if (segmentEnd == text.length()) {
                break;
            }

            if (
                    text.charAt(segmentEnd) == '\r'
                            && segmentEnd + 1 < text.length()
                            && text.charAt(segmentEnd + 1) == '\n'
            ) {
                segmentStart = segmentEnd + 2;
            } else {
                segmentStart = segmentEnd + 1;
            }

            segmentIndex++;
        }

        return Optional.empty();
    }
}
