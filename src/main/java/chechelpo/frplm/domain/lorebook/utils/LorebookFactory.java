package chechelpo.frplm.domain.lorebook.utils;

import chechelpo.frplm.annotations.Factory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.NotNull;

@Factory
public final class LorebookFactory extends EntityFactory<LorebooksRecord, Lorebook, LorebookRepository> {
    LorebookFactory(LorebookRepository repository) {
        super(repository);
    }

    @Override
    protected Lorebook instantiate(@NotNull EntityKey<LorebooksRecord> key) {
        return new Lorebook(key, repository);
    }
}
