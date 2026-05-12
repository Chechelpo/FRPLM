package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Store
final class HostStore extends ABSEntityStore<ApiHostsRecord> {
    HostStore(@NotNull DSLContext ctx) {
        super(ctx, API_HOSTS, EntityTypes.Types.API_HOSTS);
    }
}
