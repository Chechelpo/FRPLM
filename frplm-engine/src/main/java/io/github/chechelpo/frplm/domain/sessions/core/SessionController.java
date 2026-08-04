package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.SESSIONS_URL;

@RestController
@RequestMapping(SESSIONS_URL)
final class SessionController extends EntityController<SessionsRecord, SessionService> {
    SessionController(SessionService store, DTOMapper<SessionsRecord> mapper) {
        super(store, mapper);
    }
    private record stats(int messageNumber){}
    @GetMapping(ENTITY_PATH + "/stats/{id}")
    ResponseEntity<stats> stats(@PathVariable int id) {
        return null;
    }
}
