package io.github.chechelpo.frplm.core.prompt;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.prompt.building.PromptResult;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookController;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryController;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LorebookImpl;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.API_BASE;

@RestController
@Component
@RequestMapping(API_BASE + "/prompts")
final class PromptController {

    private final PromptService promptService;
    private final LorebookController lorebookController;
    private final EntryController entryController;

    public PromptController(PromptService promptService, LorebookController lorebookController, EntryController entryController) {
        this.promptService = promptService;
        this.lorebookController = lorebookController;
        this.entryController = entryController;
    }

    private record PromptDTO(
            EntityController.EntityDTO[] lorebooks,
            EntityController.EntityDTO[] activatedEntries,
            ChatCompletionRequest rawRequest
    ) {}
    @GetMapping(
            value = "/new/{sessionID}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<PromptDTO> getPrompt(@PathVariable int sessionID) throws EntityNotFound {
        PromptResult newPrompt = promptService.getNewPrompt(sessionID);
        return ResponseEntity.ok(
                new PromptDTO(
                        lorebookController.wrapEntities(
                                newPrompt.lorebooksManager().usedLorebooks().stream()
                                        .map(LorebookImpl.class::cast)
                                        .map(LorebookImpl::getRecord)
                                        .toList()
                        ),
                        entryController.wrapEntities(newPrompt.lorebooksManager().activatedEntries()),
                        newPrompt.request()
                )
        );
    }
}
