package chechelpo.frplm.pipelines;

import chechelpo.frplm.domain.sessions.messages.core.MessageController;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import jakarta.websocket.server.PathParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static chechelpo.frplm.domain.EntityTypes.API_BASE;

@RestController
@RequestMapping(API_BASE + "/engine")
final class EngineController {
    private final EngineHolder engine;
    private final MessageController messageController;

    EngineController(EngineHolder engine, MessageController messages) {
        this.engine = engine;
        this.messageController = messages;
    }

    public record GenerationOptions(
            boolean streaming,
            boolean autoResponse,
            boolean debugPrompt,
            Optional<ChatCompletionRequest> prompt
    ) {
    }

    @GetMapping("/prompt/{sessionID}")
    ResponseEntity<ChatCompletionRequest> getPrompt(@PathVariable int sessionID) throws EntityNotFound {
        return ResponseEntity.ok(engine.getNewPrompt(sessionID));
    }

    @PostMapping("/{sessionID}")
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
            @RequestBody GenerationOptions options
    ) throws EntityNotFound {
        if (options.streaming) throw new UnsupportedOperationException("streaming not supported");
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
