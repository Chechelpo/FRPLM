package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.MESSAGES_URL;

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
