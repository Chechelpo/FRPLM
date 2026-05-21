package chechelpo.frplm.domain.prompts.section.utils;

import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatMessage;
import chechelpo.frplm.domain.lorebook.entry.utils.Entry;
import chechelpo.frplm.domain.prompts.template.utils.PromptRenderContext;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

public final class EntityPromptSection extends Entity<PromptSectionRecord, PromptSectionRepository> implements PromptSection{
    /** outletID -> inject location */
    private Int2ObjectArrayMap<DetectedOutlet> detectedOutlets = new Int2ObjectArrayMap<>();
    private static final Pattern OUTLET_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    EntityPromptSection(EntityKey<PromptSectionRecord> key, PromptSectionRepository repository) {
        super(key, repository);
    }

    @Override
    public short position() {
        return this.get(PROMPT_SECTION.POSITION);
    }

    @Override
    public boolean active() {
        return this.get(PROMPT_SECTION.ACTIVE);
    }

    @Override
    public @NotNull @Unmodifiable IntSet getDetectedOutlets() {
        String content = this.get(PROMPT_SECTION.CONTENT);

        if (content == null || content.isBlank()) {
            this.detectedOutlets = new Int2ObjectArrayMap<>();
            return IntSets.EMPTY_SET;
        }

        IntArraySet outletIDs = new IntArraySet();
        Int2ObjectArrayMap<DetectedOutlet> locations = new Int2ObjectArrayMap<>();

        Matcher matcher = OUTLET_PATTERN.matcher(content);

        while (matcher.find()) {
            String outletName = matcher.group(1).trim();

            if (outletName.isEmpty()) {
                continue;
            }

            Optional<Integer> outletID = repository.getOutletID(outletName);

            if (outletID.isEmpty()) {
                continue;
            }

            int id = outletID.get();

            outletIDs.add(id);

            /*
             * If the same outlet appears more than once, this keeps the first
             * detected location. Replace with plain put(...) if the last
             * occurrence should win instead.
             */
            if (!locations.containsKey(id)) {
                locations.put(
                        id,
                        new DetectedOutlet(
                                id,
                                position(),
                                matcher.start()
                        )
                );
            }
        }

        this.detectedOutlets = locations;
        return IntSets.unmodifiable(outletIDs);
    }

    @Override
    public @NotNull IntSet getDetectedKeywords() {
        return null;
    }

    @Override
    public void inject(@NotNull Int2ObjectMap<Entry> entries) {

    }

    @Override
    public @NotNull List<ChatMessage> render(@NotNull PromptRenderContext context) {
        if (!active()) {
            return List.of();
        }
        return null;
    }
}