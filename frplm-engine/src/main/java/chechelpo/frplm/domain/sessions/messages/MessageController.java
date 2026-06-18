package chechelpo.frplm.domain.sessions.messages;

import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.utils.CRUDActionResult;
import jakarta.websocket.server.PathParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static chechelpo.frplm.domain.EntityTypes.MESSAGES_URL;
import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@RestController
@RequestMapping(MESSAGES_URL)
public final class MessageController extends EntityController<MessagesRecord, MessageService> {
    MessageController(MessageService service) {
        super(service);
    }

    record ResponseDTO(int activeResponse, int withinResponse, int responseCount){}
    @GetMapping("/response")
    ResponseEntity<ResponseDTO> getActiveResponseInfo(@RequestParam int sessionId, @RequestParam int tick_num) {
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
    }

}
