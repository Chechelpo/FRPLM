package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.connection.llm.LLMService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;


@Service
public class TemplateService extends EntityService<PromptTemplateRecord, TemplateStore> {
    private final LLMService llmService;

    TemplateService(TemplateStore store, EventBus bus, LLMService llmService) {
        super(store, bus);
        this.llmService = llmService;
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public Optional<PromptTemplateRecord> getOf(@NotNull SessionsRecord record) throws EntityNotFound {
        if (record.getMainPrompt() == null) return Optional.empty();
        return this.find(EntityKey.of(PROMPT_TEMPLATE.ID, record.getMainPrompt().shortValue()));
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<PromptTemplateRecord> target, @NotNull EntityDataPayload<PromptTemplateRecord> data, long operationID) {
        if (data.assignsField(PROMPT_TEMPLATE.MAX_TOKENS)) validateMaxTokens(target, data);
        if (data.assignsField(PROMPT_TEMPLATE.CONNECTION_ID)) updateToMaxTokensConnection(data);

        super.beforeUpdate(target, data, operationID);
    }

    private void updateToMaxTokensConnection(@NotNull EntityDataPayload<PromptTemplateRecord> data) {
        data.set(PROMPT_TEMPLATE.MAX_TOKENS,
                llmService.getValueOf(LLM_CONNECTION.MAX_TOKENS,
                        EntityKey.of(LLM_CONNECTION.ID, data.requireValue(PROMPT_TEMPLATE.CONNECTION_ID))
                ).orElseThrow(() -> new UnexpectedException("Setting new connection ID with no max_tokens", Severity.USER))
        );
    }

    private void validateMaxTokens(@NotNull EntityKey<PromptTemplateRecord> target, @NotNull EntityDataPayload<PromptTemplateRecord> data) {
        LlmConnectionRecord connection = llmService.fromTemplate(this.find(target)
                .orElseThrow(() -> new EntityNotFound("No template with key " + target, Severity.USER))
        ).orElseThrow(() -> new NotInitialized("Modifying max tokens with no connection assigned", Severity.EXPECTED));

        if (connection.getMaxTokens() < data.requireValue(PROMPT_TEMPLATE.MAX_TOKENS))
            throw new InvalidValue("Tokens of template larger than LLMs connection max tokens");
    }

    @TransactionalEventListener
    void updateToMaxTokens(CRUDCommittedEvent.@NotNull UpdatedEntity<?> rawEvent){
        if (rawEvent.type() != EntityConfigs.Types.LLM_CONNECTION) return;

        CRUDCommittedEvent.UpdatedEntity<LlmConnectionRecord> event =
                (CRUDCommittedEvent.UpdatedEntity<LlmConnectionRecord>) rawEvent;

        if (!event.updatedData().assignsField(LLM_CONNECTION.MAX_TOKENS)) return;

        this.store.updateMaxTokens(
                event.target().requireValue(LLM_CONNECTION.ID),
                event.updatedData().requireValue(LLM_CONNECTION.MAX_TOKENS)
        );
    }
}
