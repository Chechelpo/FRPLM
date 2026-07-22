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
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;
import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;


@Service
public class TemplateService extends EntityService<PromptTemplateRecord, TemplateStore> {
    private final LLMService llmService;

    TemplateService(TemplateStore store, EventBus bus, LLMService llmService) {
        super(store, bus);
        this.llmService = llmService;
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public FindResult<PromptTemplateRecord> getOf(@NotNull SessionsRecord record) throws EntityNotFound {
        if (record.getMainPrompt() == null) return FindResult.notFound(null);
        return this.find(EntityKey.of(PROMPT_TEMPLATE.ID, record.getMainPrompt().shortValue()));
    }


    @Override
    protected void beforeCreate(EntityDataPayload<PromptTemplateRecord> data, long operationID) {
        if (data.assignsField(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET) || data.assignsField(PROMPT_TEMPLATE.LOREBOOKS_BUDGET))
            validateCreationOfBudget(data);
        super.beforeCreate(data, operationID);
    }

    private void validateCreationOfBudget(EntityDataPayload<PromptTemplateRecord> data){
        boolean assignedField = false;
        if (data.assignsField(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET) && !data.assignsField(PROMPT_TEMPLATE.LOREBOOKS_BUDGET)){
            assignedField = true;
            data.set(PROMPT_TEMPLATE.LOREBOOKS_BUDGET, 1F - data.requireValue(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET));
        }
        if (data.assignsField(PROMPT_TEMPLATE.LOREBOOKS_BUDGET) && !data.assignsField(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET)){
            assignedField = true;
            data.set(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET, 1F - data.requireValue(PROMPT_TEMPLATE.LOREBOOKS_BUDGET));
        }

        validateBudgetAssignation(
                null,
                data.requireValue(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET),
                data.requireValue(PROMPT_TEMPLATE.LOREBOOKS_BUDGET)
        );
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<PromptTemplateRecord> target, @NotNull EntityDataPayload<PromptTemplateRecord> data, long operationID) {
        if (data.assignsField(PROMPT_TEMPLATE.MAX_TOKENS)) validateMaxTokens(target, data);
        if (data.assignsField(PROMPT_TEMPLATE.CONNECTION_ID)) updateToMaxTokensConnection(data);
        validateBudgetAssignation(
                target,
                data.getValue(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET).orElse(null),
                data.getValue(PROMPT_TEMPLATE.LOREBOOKS_BUDGET).orElse(null)
        );

        super.beforeUpdate(target, data, operationID);
    }

    private void updateToMaxTokensConnection(@NotNull EntityDataPayload<PromptTemplateRecord> data) {
        data.set(PROMPT_TEMPLATE.MAX_TOKENS,
                llmService.getValueOf(LLM_CONNECTION.MAX_TOKENS,
                        EntityKey.of(LLM_CONNECTION.ID, data.requireValue(PROMPT_TEMPLATE.CONNECTION_ID))
                ).orElseThrow(() -> new UnexpectedException("Setting new connection ID with no maxTokens", Severity.USER))
        );
    }

    private void validateMaxTokens(@NotNull EntityKey<PromptTemplateRecord> target, @NotNull EntityDataPayload<PromptTemplateRecord> data) {
        LlmConnectionRecord connection = llmService.fromTemplate(this.find(target)
                .orElseThrow("Couldn't find connection of template " + target, Severity.USER)
        ).orElseThrow(notFound ->
                new NotInitialized("Modifying max tokens of template %s no connection assigned".formatted(target), Severity.USER)
        );

        if (connection.getMaxTokens() < data.requireValue(PROMPT_TEMPLATE.MAX_TOKENS))
            throw new InvalidValue("Tokens of template larger than LLMs connection max tokens");
    }

    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    private void validateBudgetAssignation(
            EntityKey<PromptTemplateRecord> ofTemplate,
            @Nullable Float chatHistoryBudget,
            @Nullable Float lorebookBudget
    ){
        if (chatHistoryBudget == null && lorebookBudget == null) return;
        if (( chatHistoryBudget == null || lorebookBudget == null ) && ofTemplate == null)
            throw new IllegalArgumentException("Target key must be non null if chat history budget is null or lorebook budget");

        if (chatHistoryBudget == null)
            chatHistoryBudget = getValueOf(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET, ofTemplate)
                    .orElseThrow(() -> new EntityNotFound("No template with key: " + ofTemplate, Severity.SYSTEM));
        if (lorebookBudget == null)
            lorebookBudget = getValueOf(PROMPT_TEMPLATE.LOREBOOKS_BUDGET, ofTemplate)
                    .orElseThrow(() -> new EntityNotFound("No template with key: " + ofTemplate, Severity.SYSTEM));

        if (lorebookBudget + chatHistoryBudget > 1D)
            throw new InvalidValue("Lorebook budget and history budget sum to more than 1", Severity.USER);
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
