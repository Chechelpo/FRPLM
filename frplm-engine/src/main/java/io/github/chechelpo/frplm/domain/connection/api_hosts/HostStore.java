package io.github.chechelpo.frplm.domain.connection.api_hosts;

import io.github.chechelpo.frplm.annotations.Store;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Store
final class HostStore extends EntityStore<ApiHostsRecord> {
    HostStore(@NotNull DSLContext ctx) {
        super(ctx, API_HOSTS, EntityConfigs.Types.API_HOSTS);
    }

    ApiHostsRecord getWithName(String url){
        return ctx.selectFrom(main_table)
                .where(API_HOSTS.HOST_URL.eq(url))
                .fetchOne();
    }
}
