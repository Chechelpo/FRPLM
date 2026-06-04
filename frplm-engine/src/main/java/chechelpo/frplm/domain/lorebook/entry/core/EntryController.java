package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.domain.EntityTypes.ENTRIES_URL;
import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@RestController
@RequestMapping(ENTRIES_URL)
final class EntryController extends EntityController<EntryRecord, EntryService> {
    EntryController(EntryService service, LorebookService lorebookService) {
        super(service);
    }

    /**
     * Temporary work-around while queries are implemented
     */
    @GetMapping(ENTITY_PATH + "/{lorebook_id}")
    ResponseEntity<EntityDTO[]> ofLorebook(@PathVariable("lorebook_id") Integer lorebookId) {
        return ResponseEntity.ok(
                wrapEntities(service.of(lorebookId))
        );
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

}
