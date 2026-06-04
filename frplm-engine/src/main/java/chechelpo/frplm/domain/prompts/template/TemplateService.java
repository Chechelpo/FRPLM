package chechelpo.frplm.domain.prompts.template;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;


@Service
public class TemplateService extends EntityService<PromptTemplateRecord, TemplateStore> {
    TemplateService(TemplateStore store, EventBus bus) {
        super(store, bus);
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public Optional<PromptTemplateRecord> getOf(@NotNull SessionsRecord record) throws EntityNotFound {
        if (record.getMainPrompt() == null) return Optional.empty();
        return this.find(EntityKey.of(PROMPT_TEMPLATE.ID, record.getMainPrompt().shortValue()));
    }

}
