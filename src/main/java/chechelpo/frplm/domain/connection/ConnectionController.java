package chechelpo.frplm.domain.connection;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.ConnectionRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.EntityTypes.CONNECTION_URL;

@RestController
@RequestMapping(CONNECTION_URL)
final class ConnectionController extends ABSEntityController<ConnectionRecord, ConnectionService> {
    ConnectionController(ConnectionService service) {
        super(EntityTypes.Types.CONNECTION, service);
    }
}
