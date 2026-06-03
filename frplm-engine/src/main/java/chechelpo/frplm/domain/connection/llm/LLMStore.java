package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.LlmConnection;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

@Store
final class LLMStore extends EntityStore<LlmConnectionRecord> {
    LLMStore(@NotNull DSLContext ctx) {
        super(ctx, LlmConnection.LLM_CONNECTION, EntityTypes.Types.LLM_CONNECTION);
    }
}
