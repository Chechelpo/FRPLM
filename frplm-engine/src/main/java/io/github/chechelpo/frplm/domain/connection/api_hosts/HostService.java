package io.github.chechelpo.frplm.domain.connection.api_hosts;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.domain.connection.llm.LLMBackend;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.ApiHosts;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Service
public class HostService extends EntityService<ApiHostsRecord, HostStore> {
    HostService(HostStore store, FieldValidator<ApiHostsRecord> validator, EventBus eventBus) {
        super(store, validator, eventBus);
    }

    @Override
    public boolean delete(@NotNull EntityKey<ApiHostsRecord> id) {
        if (LLMBackend.isStandardBackend(id.require(ApiHosts.API_HOSTS.ID))){
            log.error("Tried to delete standard backend: {}", LLMBackend.get(id.require(ApiHosts.API_HOSTS.ID)));
            return false;
        }
        return super.delete(id);
    }

    public ApiHostsRecord createOrGetWithHost(String url){
        ApiHostsRecord withName = store.getWithName(url);
        if (withName == null)
            withName = store.createAndGet(
                    EntityDataPayload.<ApiHostsRecord>builder()
                            .set(API_HOSTS.HOST_URL, url)
                            .build()
            );

        return withName;
    }
}
