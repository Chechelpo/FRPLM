package chechelpo.frplm.core.engine;

import chechelpo.frplm.domain.lorebook.core.LorebookController;
import chechelpo.frplm.domain.lorebook.entry.core.EntryController;
import chechelpo.frplm.domain.sessions.messages.MessageController;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static chechelpo.frplm.domain.EntityTypes.API_BASE;

@RestController
@RequestMapping(API_BASE + "/engine")
final class GeneratorController {
    private final EngineHolder engine;
    private final MessageController messageController;
    private final LorebookController lorebooks;
    private final EntryController entryController;

    GeneratorController(
            EngineHolder engine,
            MessageController messages,
            LorebookController lorebooks,
            EntryController entries
    ) {
        this.engine = engine;
        this.messageController = messages;
        this.lorebooks = lorebooks;
        this.entryController = entries;
    }

    public record GenerationOptions(
            boolean streaming,
            ChatCompletionRequest prompt
    ) {}



    @PostMapping("/generate/{sessionID}")
    ResponseEntity<MessageController.EntityDTO> generate(
            @PathVariable int sessionID,
            @RequestBody GenerationOptions options
    ) throws EntityNotFound {
        if (options.streaming) throw new UnsupportedOperationException("streaming not supported");
        return ResponseEntity.ok(
                messageController.wrapEntity(
                        engine.generateNewMessage(
                                sessionID,
                                options.prompt
                        )
                )
        );
    }

    @PostMapping("/regenerate")
    ResponseEntity<MessageController.EntityDTO> regenerate(
            @RequestParam int sessionID,
            @RequestParam int tick_num,
            @RequestBody(required = false) GenerationOptions options
    ) throws EntityNotFound {
        if (options != null && options.streaming) throw new UnsupportedOperationException("streaming not supported");
        return ResponseEntity.ok(
                messageController.wrapEntity(
                        engine.regenerate(
                                sessionID,
                                tick_num
                        )
                )
        );
    }
}
