package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewEntryOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

@Component
public class EntryMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final EntryKeywordService entryKeywordService;
    private final OutletService outletService;

    public EntryMapper(EntryKeywordService entryKeywordService, OutletService outletService) {
        this.entryKeywordService = entryKeywordService;
        this.outletService = outletService;
    }

    record EntryJSON(
            String name,
            String content,
            String embed_text,
            Set<String> keywords,
            String outlet,

            Boolean enabled,

            Short probability,
            Integer delay,
            Integer cooldown,
            Integer stick_through,
            Short injection_order,
            short strategy,

            Boolean prevent_further_recursion,
            Boolean non_recursable,
            Boolean delay_until_recursion,

            Short scan_depth,
            Short group_id
    ) {
    }

    public JsonNode jsonFrom(@NonNull EntryRecord entry) {
        return MAPPER.valueToTree(new EntryJSON(
                        entry.getName(),
                        entry.getContent(),
                        entry.getEmbedText(),
                        fetchKeywords(entry),
                        fetchOutlet(entry),

                        entry.getEnabled(),
                        entry.getProbability(),
                        entry.getDelay(),
                        entry.getCooldown(),
                        entry.getStickThrough(),
                        entry.getPosition(),
                        entry.getStrategy(),
                        entry.getPreventFurtherRecursion(),
                        entry.getNonRecursable(),
                        entry.getDelayUntilRecursion(),
                        entry.getScanDepth(),
                        entry.getGroupId()
                )
        );
    }

    Set<String> fetchKeywords(@NonNull EntryRecord entry) {
        return entryKeywordService.keywordsOfEntry(entry.getLorebookId(), entry.getEntryId());
    }

    String fetchOutlet(@NonNull EntryRecord entry) {
        if (entry.getOutlet() == null) return null;
        return outletService.getOutletName(entry.getOutlet())
                .orElse(null);
    }

    public NewEntryOrder orderFrom(JsonNode node) {
        EntryJSON json = MAPPER.treeToValue(node, EntryJSON.class);
        int outletId = outletService.getOrCreateOutlet(json.outlet);

        return new NewEntryOrder(
                json.keywords,
                EntityDataPayload.<EntryRecord>builder()
                        .set(ENTRY.NAME, json.name)
                        .set(ENTRY.EMBED_TEXT, json.embed_text)
                        .set(ENTRY.CONTENT, json.content)
                        .set(ENTRY.OUTLET, outletId)

                        .set(ENTRY.ENABLED, json.enabled)
                        .set(ENTRY.PROBABILITY, json.probability)
                        .set(ENTRY.DELAY, json.delay)
                        .set(ENTRY.COOLDOWN, json.cooldown)
                        .set(ENTRY.STICK_THROUGH, json.stick_through)
                        .set(ENTRY.POSITION, (short) json.injection_order)
                        .set(ENTRY.STRATEGY, (short) json.strategy)

                        .set(ENTRY.PREVENT_FURTHER_RECURSION, json.prevent_further_recursion)
                        .set(ENTRY.NON_RECURSABLE, json.non_recursable)
                        .set(ENTRY.DELAY_UNTIL_RECURSION, json.delay_until_recursion)

                        .set(ENTRY.SCAN_DEPTH, json.scan_depth)
                        .set(ENTRY.GROUP_ID, json.group_id)
                        .build()
        );
    }
}
