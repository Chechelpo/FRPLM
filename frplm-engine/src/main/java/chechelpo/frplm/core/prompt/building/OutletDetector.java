package chechelpo.frplm.core.prompt.building;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record2;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OutletDetector {
    private final Int2ObjectMap<DetectedOutlet> detectedOutlets = new Int2ObjectArrayMap<>(10);

    public OutletDetector(){}

    record DetectedOutlet(int outletId, int segmentIndex, int charOffset) {}
    public void detectLorebookOutlets(String text, @NonNull LorebooksManager manager){
        Result<Record2<Integer, String>> lorebookOutlets = manager.getOutlets();
        lorebookOutlets.forEach(outlet ->
            getOutletLocation(outlet.component1(), text, StandardOutlet.asPattern(outlet.component2()))
                    .ifPresent(
                            detected ->
                                    detectedOutlets.putIfAbsent((int) outlet.component1(), detected)
                    )
        );
    }

    @Contract(pure = true)
    static @NotNull Optional<DetectedOutlet> getOutletLocation(
            int outletId,
            @NotNull String text,
            @NotNull Pattern pattern
    ) {
        Objects.requireNonNull(text, "Text to scan is null");
        Objects.requireNonNull(pattern, "Pattern is null");

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
