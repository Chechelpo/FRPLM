package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.springframework.stereotype.Service;

@Service
public class HostService extends EntityService<ApiHostsRecord, HostStore> {
    HostService(HostStore store, EventBus eventBus) {
        super(store, eventBus);
    }
}
