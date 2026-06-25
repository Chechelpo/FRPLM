package chechelpo.frplm.utils.json_mappers;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.utils.json_mappers.orders.NewLorebookOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public final class LorebookMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final OutletService outlets;
    private final EntryService entryService;
    private final EntryMapper entryMapper;

    LorebookMapper(OutletService outlets, EntryService entryService, EntryMapper entryMapper){
        this.outlets = outlets;
        this.entryService = entryService;
        this.entryMapper = entryMapper;
    }

    public record LorebookJSON(
            String name,
            String default_outlet_id,
            List<JsonNode> entries
    ){}

    public JsonNode jsonFrom(@NonNull LorebooksRecord record){
        return MAPPER.valueToTree(new LorebookJSON(
                record.getName(),
                fetchDefaultOutlet(record),
                entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, record.getId()))
                        .stream()
                        .map(entryMapper::jsonFrom)
                        .toList()
        ));
    }

    @NonNull String fetchDefaultOutlet(@NonNull LorebooksRecord record){
        return outlets.getOutletName(record.getDefaultOutletId())
                .orElseThrow(() -> new EntityNotFound("No default outlet name with id: " + record.getDefaultOutletId(), Severity.SYSTEM));
    }

    @NonNull
    public NewLorebookOrder orderFrom(JsonNode node){
        LorebookJSON lorebookJSON = MAPPER.treeToValue(node, LorebookJSON.class);
        int outletId = outlets.getOrCreateOutlet(lorebookJSON.default_outlet_id);
        return new NewLorebookOrder(
                EntityDataPayload.<LorebooksRecord>builder()
                        .set(LOREBOOKS.NAME, lorebookJSON.name)
                        .set(LOREBOOKS.DEFAULT_OUTLET_ID, outletId)
                        .build(),
                lorebookJSON.entries.stream()
                        .map(entryMapper::orderFrom)
                        .toList()
        );
    }
}
