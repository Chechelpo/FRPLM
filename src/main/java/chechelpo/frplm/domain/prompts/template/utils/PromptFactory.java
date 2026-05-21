package chechelpo.frplm.domain.prompts.template.utils;

import chechelpo.frplm.annotations.Factory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;

@Factory
public final class PromptFactory extends EntityFactory<PromptTemplateRecord, Prompt, PromptRepository> {
    PromptFactory(PromptRepository repository) {
        super(repository);
    }

    @Override
    protected Prompt instantiate(@NotNull EntityKey<PromptTemplateRecord> key) {
        return new Prompt(key, repository);
    }
}
