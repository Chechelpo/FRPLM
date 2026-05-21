package chechelpo.frplm.domain.sessions.core.microservices;

import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.domain.EntityTypes.SESSIONS_URL;

@RestController
@RequestMapping(SESSIONS_URL)
final class SessionController extends EntityController<SessionsRecord, SessionService> {
    SessionController(SessionService store) {
        super(store);
    }
    private record stats(int messageNumber){}
    @GetMapping(ENTITY_PATH + "/stats/{id}")
    ResponseEntity<stats> stats(@PathVariable int id) {
        return null;
    }
}
