package chechelpo.frplm.domain.lorebook.entry.utils;

import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
final class EntryRepository extends EntityRepository<EntryRecord, EntryService> {
    EntryRepository(EntryService service) {
        super(service);
    }
    @NotNull EntryService getService() {
        return service;
    }
}
