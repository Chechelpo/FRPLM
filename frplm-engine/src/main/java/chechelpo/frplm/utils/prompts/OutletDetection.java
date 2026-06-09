package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OutletDetection {
    private OutletDetection() {
    }

    @Contract(pure = true)
    public static @NotNull List<DetectedOutlet> getDetectedOutlets(
            IntObjectPair<String>[] outlets,
            String message
    ) {
        if (outlets == null || message == null) throw new IllegalArgumentException("outlets or message are null");
        if (message.isEmpty()) return List.of();
        assert areUnique(outlets) : "Outlet ids are not unique";

        List<DetectedOutlet> detectedOutlets = new ArrayList<>(outlets.length);

        for (IntObjectPair<String> outletPair : outlets) {
            int outletID = outletPair.firstInt();
            String outletName = outletPair.second();

            getOutletLocation(
                    outletID,
                    message,
                    StandardOutlet.asPattern(outletName)
            ).ifPresent(detectedOutlets::add);
        }

        return detectedOutlets;
    }

    @Contract(pure = true)
    private static @NotNull Optional<DetectedOutlet> getOutletLocation(
            int outletId,
            @NotNull String text,
            @NotNull Pattern pattern
    ) {
        int segmentIndex = 0;
        int segmentStart = 0;

        while (segmentStart <= text.length()) {
            int segmentEnd = segmentStart;

            // Finds next newline.
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
                return Optional.of(new DetectedOutlet(
                        outletId,
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

    private static boolean areUnique(IntObjectPair<String> @NotNull [] outlets) {
        IntSet outletIds = IntSetFactory.ofLength(outlets.length);
        for (IntObjectPair<String> outlet : outlets) {
            if (outletIds.contains(outlet.firstInt())) return false;
            else outletIds.add(outlet.firstInt());
        }
        return true;
    }


}
