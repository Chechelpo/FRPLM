package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSES;

@Store
class ResponseStore extends EntityStore<ResponsesRecord> {
    protected ResponseStore(@NotNull DSLContext ctx) {
        super(ctx, RESPONSES, EntityConfigs.Types.RESPONSES);
    }
}
