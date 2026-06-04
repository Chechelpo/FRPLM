package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
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

    @GetMapping("ofSession/{sessionID}")
    ResponseEntity<EntityDTO[]> getMessageBySessionID(@PathVariable("sessionID") int sessionID) {
        return ResponseEntity.ok(
                wrapEntities(
                        this.service.getMatching(
                                EntityKey.of(MESSAGES.SESSION_ID, sessionID)
                        )
                )
        );
    }


    @PostMapping("generate/newMessage/{sessionID}")
    ResponseEntity<String> generateNewMessage(
            @PathVariable("sessionID") int sessionID,
            @RequestBody(required = false) ChatCompletionRequest prompt
    ) {
        return null;
    }
}
