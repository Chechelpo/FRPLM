package chechelpo.frplm.utils.importers.lorebooks;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

public final class STLorebookImporter {
    private STLorebookImporter() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private record STLorebook(
            Map<String, STEntry> entries
    ){}
    private record STEntryWrapper(
            int number
    ){}
    private record STEntry(
            int uid,

            @JsonProperty("key")
            List<String> keys,

            @JsonProperty("keysecondary")
            List<String> keySecondary,

            String comment,
            String content,

            boolean constant,
            boolean selective,
            int order,
            int position,
            boolean disable,

            int displayIndex,
            boolean addMemo,

            String group,
            boolean groupOverride,
            int groupWeight,

            Integer sticky,
            Integer cooldown,
            Integer delay,

            int probability,
            int depth,
            boolean useProbability,

            String role,

            boolean vectorized,
            boolean excludeRecursion,
            boolean preventRecursion,
            boolean delayUntilRecursion,

            Boolean scanDepth,
            Boolean caseSensitive,
            Boolean matchWholeWords,
            Boolean useGroupScoring,

            String automationId
    ) {}


    /**
     * @param sTLorebookJSON with fields described by {@link STEntry} and {@link STLorebook}
     * @return a list of parsed entries, ready to inject into a lorebook
     */
    @Contract(pure = true)
    public static List<NewEntryOrder> getEntries(JsonNode sTLorebookJSON) {
        if (sTLorebookJSON == null || sTLorebookJSON.isNull()) {
            return List.of();
        }

        final STLorebook lorebook;
        try {
            lorebook = MAPPER.treeToValue(sTLorebookJSON, STLorebook.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SillyTavern lorebook JSON", e);
        }

        if (lorebook.entries() == null || lorebook.entries().isEmpty()) {
            return List.of();
        }

        return lorebook.entries()
                .values()
                .stream()
                .map(entry -> new NewEntryOrder(entry.keys, toEntryPayload(entry)))
                .toList();
    }

    @Contract("_ -> new")
    static @NotNull EntityDataPayload<EntryRecord> toEntryPayload(@NotNull STEntry stEntry) {
        return EntityDataPayload.<EntryRecord>builder()
                .set(ENTRY.NAME, stEntry.comment)
                .set(ENTRY.CONTENT, stEntry.content)

                .set(ENTRY.STRATEGY, getEntryStrategy(stEntry).stable_id)
                .set(ENTRY.PROBABILITY, (short) stEntry.probability )

                .set(ENTRY.COOLDOWN, stEntry.cooldown == null ? 0 : stEntry.cooldown)
                .set(ENTRY.DELAY, stEntry.delay == null ? 0 : stEntry.delay)

                .set(ENTRY.PREVENT_FURTHER_RECURSION, stEntry.preventRecursion)
                .build();
    }

    @Contract(pure = true)
    static ActivationStrategy getEntryStrategy(@NotNull STEntry entry) {
        if (entry.constant) return ActivationStrategy.CONSTANT;
        if (entry.vectorized) return ActivationStrategy.EMBEDDING;
        return ActivationStrategy.COMMON;
    }
}
