package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.domain.EntityTypes.ENTRIES_URL;
import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@RestController
@Component
@RequestMapping(ENTRIES_URL)
public final class EntryController extends EntityController<EntryRecord, EntryService> {
    EntryController(EntryService service, LorebookService lorebookService) {
        super(service);
    }

    @PatchMapping(ENTITY_PATH + "/{lorebook_id}/{entryID}")
    ResponseEntity<Boolean> updateOutlet(
            @PathVariable("lorebook_id") int lorebookId,
            @PathVariable("entryID") int entryId,
            @RequestBody String outlet
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
    ResponseEntity<EntityDTO[]> importLorebookEntries(@PathVariable int lorebookID, @RequestBody JsonNode file){
        return ResponseEntity.ok(
                wrapEntities(
                        service.importEntriesFromJSON(lorebookID, file)
                )
        );
    }

}
