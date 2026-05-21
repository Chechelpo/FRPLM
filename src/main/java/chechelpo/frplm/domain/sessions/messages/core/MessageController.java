package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.MESSAGES_URL;

@RestController
@RequestMapping(MESSAGES_URL)
final class MessageController extends EntityController<MessagesRecord, MessageService> {
    MessageController(MessageService service) {
        super(service);
    }
}
