package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.config.controllers.EntityTypes.ENTRIES_URL;

@RestController
@RequestMapping(ENTRIES_URL)
final class EntryController extends ABSEntityController<EntryRecord, EntryService> {

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
}
