package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@Service
public class LLMService extends EntityService<LlmConnectionRecord, LLMStore> {
    LLMService(LLMStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Transactional(readOnly = true)
    public Optional<LlmConnectionRecord> fromTemplate(@NotNull PromptTemplateRecord template) {
        return this.find(EntityKey.of(LLM_CONNECTION.ID, template.getConnectionId().intValue()));
    }
}
