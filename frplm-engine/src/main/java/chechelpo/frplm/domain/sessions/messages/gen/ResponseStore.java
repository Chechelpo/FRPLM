package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSES;

@Store
final class ResponseStore extends EntityStore<ResponsesRecord> {
    ResponseStore(@NotNull DSLContext ctx) {
        super(ctx, RESPONSES, EntityTypes.Types.RESPONSES);
    }
}
