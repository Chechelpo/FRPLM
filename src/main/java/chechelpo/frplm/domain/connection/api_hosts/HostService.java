package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.springframework.stereotype.Service;

@Service
public final class HostService extends ABSEntityService<ApiHostsRecord, HostStore> {
    HostService(HostStore store) {
        super(store);
    }
}
