package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.extensions.api.utils.FindResult;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import io.github.chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.PromptBudget;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.GenerationConfig;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.GenerationParameters;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ReasoningEffort;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

public class PromptImpl extends StandaloneEntity<PromptTemplateRecord> implements PromptSnapshot {
    public PromptImpl(PromptTemplateRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public FindResult<ConnectionSnapshot, ?, ?> getAssignedConnection() {
        if (this.record.getConnectionId() == null) 
            return FindResult.empty("Prompt " + record.getName() + " has no assigned connection");

        return context.connections().find(
                EntityKey.of(LLM_CONNECTION.ID, this.record.getConnectionId())
        ).mapResult(rec -> new ConnectionImpl(rec, this.context));
    }

    @Override
    public GenerationConfig getGenerationConfig() {
        return new GenerationConfig(
                record.getStreaming(),
                record.getExcludeReasoning(),
                record.getMaxTokens(),
                ReasoningEffort.fromId(record.getReasoningEffort())
        );
    }

    @Override
    public GenerationParameters getParameters() {
        return GenerationParameters.builder()
                .temperature(record.getTemperature())
                .repetitionPenalty(record.getRepetitionPenalty())
                .topK(record.getTopK())
                .topP(record.getTopP())
                .presencePenalty(record.getPresencePenalty())
                .frequencyPenalty(record.getFrequencyPenalty())
                .build();
    }

    @Override
    public PromptBudget getBudgetConfig() {
        return new PromptBudget(record.getMaxTokens(), record.getLorebooksBudget(), record.getChatHistoryBudget());
    }

    @Override
    public Reference asReference() {
        return new PromptSnapshot.Reference(this.record.getId());
    }

    @Override
    public List<PromptSectionEntitySnapshot> getSections() {
        return context.sections().getOrderedSectionsOfTemplate(this.getRecord()).stream()
                .map(section -> new PromptSectionEntityImpl(section, this.context))
                .map(PromptSectionEntitySnapshot.class::cast)
                .toList();
    }
}
