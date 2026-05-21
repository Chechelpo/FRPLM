package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.domain.EntityTypes.ENTRIES_URL;
import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@RestController
@RequestMapping(ENTRIES_URL)
final class EntryController extends EntityController<EntryRecord, EntryService> {

    EntryController(EntryService service) {
        super(EntityTypes.Types.ENTRIES, service);
    }

    /**
     * Temporary work-around while queries are implemented
     */
    @GetMapping(ENTITY_PATH + "/{lorebook_id}")
    ResponseEntity<EntityDTO[]> ofLorebook(@PathVariable("lorebook_id") Integer lorebookId) {
        return ResponseEntity.ok(
                wrapEntities(service.getOfLorebook(lorebookId))
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
                        EntityKey.ofValues(Map.of(
                                        ENTRY.ENTRY_ID, entryId,
                                        ENTRY.LOREBOOK_ID, lorebookId
                                )
                        ),
                        outlet
                )
        );
    }
}
