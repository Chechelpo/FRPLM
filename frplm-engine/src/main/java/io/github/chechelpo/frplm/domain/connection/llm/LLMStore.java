package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.LlmConnection;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

@Store
final class LLMStore extends EntityStore<LlmConnectionRecord> {
    LLMStore(@NotNull DSLContext ctx) {
        super(ctx, LlmConnection.LLM_CONNECTION, EntityConfigs.Types.LLM_CONNECTION);
    }
}
