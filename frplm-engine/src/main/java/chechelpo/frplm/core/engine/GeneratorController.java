package chechelpo.frplm.core.engine;

import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.domain.lorebook.core.LorebookController;
import chechelpo.frplm.domain.lorebook.entry.core.EntryController;
import chechelpo.frplm.domain.sessions.messages.MessageController;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.extensions.api.utils.MessagePrompt;
import chechelpo.frplm.extensions.implementations.standalone.EntryImpl;
import chechelpo.frplm.extensions.implementations.standalone.LorebookImpl;
import chechelpo.frplm.extensions.implementations.standalone.StandaloneEntity;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

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
    ) {
    }


    public record PromptDTO(
            EntityController.EntityDTO[] lorebooks,
            EntityController.EntityDTO[] activatedEntries,
            ChatCompletionRequest rawRequest
    ) { }

    @GetMapping("/prompt/{sessionID}")
    ResponseEntity<PromptDTO> getPrompt(@PathVariable int sessionID) throws EntityNotFound {
        MessagePrompt newPrompt = engine.getNewPrompt(sessionID);
        return ResponseEntity.ok(new PromptDTO(
                lorebooks.wrapEntities(
                        Arrays.stream(newPrompt.usedLorebooks())
                                .map(LorebookImpl.class::cast)
                                .map(StandaloneEntity::getRecord)
                        .toArray(LorebooksRecord[]::new)
                ),
                entryController.wrapEntities(
                        Arrays.stream(newPrompt.activatedEntries())
                                .map(EntryImpl.class::cast)
                                .map(EntryImpl::getRecord)
                                .toArray(EntryRecord[]::new)
                ),
                newPrompt.renderedRequest()
                )
        );
    }

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
