package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import static io.github.chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.ENTRIES_URL;

@RestController
@Component
@RequestMapping(ENTRIES_URL)
public final class EntryController extends EntityController<EntryRecord, EntryService> {
    EntryController(EntryService service, DTOMapper<EntryRecord> mapper) {
        super(EntityConfigs.Types.ENTRIES, service, mapper);
    }

    @PatchMapping(ENTITY_PATH + "/{lorebook_id}/{entryID}")
    ResponseEntity<Integer> updateOutlet(
            @PathVariable("lorebook_id") int lorebookId,
            @PathVariable("entryID") int entryId,
            @RequestParam String outlet
    ) {
        return ResponseEntity.ok(
                service.updateOutlet(
                        EntityKey.<EntryRecord>builder()
                                .set(ENTRY.ENTRY_ID, entryId)
                                .set(ENTRY.LOREBOOK_ID, lorebookId)
                                .build(),
                        outlet
                )
        );
    }

    @PostMapping(
            value = "/{lorebookID}/import",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<EntityDTO[]> importLorebookEntries(@PathVariable int lorebookID, @RequestBody JsonNode file) {
        return ResponseEntity.ok(
                wrapEntities(
                        service.importEntriesFromJSON(lorebookID, file)
                )
        );
    }

    @PostMapping(
            value = "/exchange"
    )
    ResponseEntity<EntityDTO> exchangeEntry(@RequestParam int fromLorebookId, @RequestParam int toLorebookId, @RequestParam int entryId) {
        try {
            return ResponseEntity.ok(
                    wrapEntity(
                            service.exchangeEntry(EntityKey.<EntryRecord>builder()
                                            .set(ENTRY.LOREBOOK_ID, fromLorebookId)
                                            .set(ENTRY.ENTRY_ID, entryId)
                                            .build(),
                                    toLorebookId
                            )
                    )
            );
        } catch (EntityNotFound e) {
            throw new EntityNotFound(e.getMessage(), Severity.USER);
        }
    }
}
