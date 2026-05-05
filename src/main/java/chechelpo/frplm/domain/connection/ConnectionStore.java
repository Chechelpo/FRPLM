package chechelpo.frplm.domain.connection;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.ConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.CONNECTION;

@Component
final class ConnectionStore extends ABSEntityStore<ConnectionRecord> {
    ConnectionStore(@NotNull DSLContext ctx) {
        super(ctx, CONNECTION, EntityTypes.Types.CONNECTION);
    }
}
