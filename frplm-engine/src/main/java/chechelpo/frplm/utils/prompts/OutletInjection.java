package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.extensions.api.utils.DetectedOutlet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static chechelpo.frplm.domain.lorebook.outlet.StandardOutlet.*;

public final class OutletInjection {
    private OutletInjection() {}

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // INJECTION
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private record PendingInjection(int start, int end, @NotNull String content) {}
    public static @NotNull String inject(
            @NotNull String originalContent,
            @NotNull List<DetectedOutlet> outlets,
            @NotNull Int2ObjectMap<List<String>> entriesByOutlet
    ) {
        if (outlets.isEmpty() || entriesByOutlet.isEmpty()) {
            return originalContent;
        }

        List<PendingInjection> pending = new ArrayList<>();

        for (DetectedOutlet outlet : outlets) {
            List<String> entries = entriesByOutlet.get(outlet.outletId());

            if (entries == null || entries.isEmpty()) {
                continue;
            }

            int start = absoluteOffsetOf(originalContent, outlet);
            int end = outletMarkerEnd(originalContent, start);

            if (start == end) {
                continue;
            }

            pending.add(new PendingInjection(
                    start,
                    end,
                    renderEntries(entries)
            ));
        }

        pending.sort(Comparator.comparingInt(PendingInjection::start).reversed());

        StringBuilder rendered = new StringBuilder(originalContent);

        for (PendingInjection injection : pending) {
            rendered.replace(
                    injection.start(),
                    injection.end(),
                    injection.content()
            );
        }

        return stripUnresolvedMacros(rendered.toString());
    }

    static @NotNull String renderEntries(@NotNull List<String> entries) {
        StringJoiner rendered = new StringJoiner("\n");

        for (String content : entries) {
            if (content != null && !content.isBlank()) {
                rendered.add(content);
            }
        }

        return rendered.toString();
    }

    @Contract(pure = true)
    private static int absoluteOffsetOf(
            @NotNull String text,
            @NotNull DetectedOutlet location
    ) {
        int segmentIndex = 0;
        int segmentStart = 0;

        while (segmentIndex < location.segmentIndex() && segmentStart < text.length()) {
            int segmentEnd = segmentStart;

            while (
                    segmentEnd < text.length()
                            && text.charAt(segmentEnd) != '\n'
                            && text.charAt(segmentEnd) != '\r'
            ) {
                segmentEnd++;
            }

            if (segmentEnd == text.length()) {
                return text.length();
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

        return Math.min(segmentStart + location.charOffset(), text.length());
    }

    @Contract(pure = true)
    private static int outletMarkerEnd(
            @NotNull String text,
            int markerStart
    ) {
        if (
                markerStart < 0
                        || markerStart + 1 >= text.length()
                        || text.charAt(markerStart) != '{'
                        || text.charAt(markerStart + 1) != '{'
        ) {
            return markerStart;
        }

        int closing = text.indexOf("}}", markerStart + 2);

        if (closing == -1) {
            return markerStart;
        }

        return closing + 2;
    }


}