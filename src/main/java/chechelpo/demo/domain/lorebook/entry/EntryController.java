package chechelpo.demo.domain.lorebook.entry;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.EntryRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.demo.config.controllers.EntityTypes.ENTRIES_URL;

@RestController
@RequestMapping(ENTRIES_URL)
final class EntryController extends ABSEntityController<EntryRecord, EntryService> {

    public EntryController(EntryService service) {
        super(EntityTypes.Types.ENTRIES, service);
    }
}
