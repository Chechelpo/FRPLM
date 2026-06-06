package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.ApiHosts;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class HostService extends EntityService<ApiHostsRecord, HostStore> {
    HostService(HostStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    public boolean delete(@NotNull EntityKey<ApiHostsRecord> id) {
        if (LLMBackend.isStandardBackend(id.requireValue(ApiHosts.API_HOSTS.ID))){
            log.error("Tried to delete standard backend: {}", LLMBackend.get(id.requireValue(ApiHosts.API_HOSTS.ID)));
            return false;
        }
        return super.delete(id);
    }
}
