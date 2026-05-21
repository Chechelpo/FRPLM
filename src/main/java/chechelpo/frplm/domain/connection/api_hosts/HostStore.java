package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Store
final class HostStore extends EntityStore<ApiHostsRecord> {
    HostStore(@NotNull DSLContext ctx) {
        super(ctx, API_HOSTS, EntityTypes.Types.API_HOSTS);
    }
}
