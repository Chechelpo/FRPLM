package chechelpo.frplm.domain.connection;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.ConnectionRecord;
import org.springframework.stereotype.Component;

@Component
public final class ConnectionService extends ABSEntityService<ConnectionRecord, ConnectionStore> {
    ConnectionService(ConnectionStore store) {
        super(store, EntityTypes.Types.CONNECTION);
    }
}
