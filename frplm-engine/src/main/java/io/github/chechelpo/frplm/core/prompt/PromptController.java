package io.github.chechelpo.frplm.core.prompt;

import io.github.chechelpo.frplm.core.entities.pseudo_services.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDTO;
import io.github.chechelpo.frplm.core.prompt.building.PromptResult;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookController;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryController;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LorebookImpl;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
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
    private final DTOMapper<LorebooksRecord> lorebookMapper;
    private final DTOMapper<EntryRecord> entryMapper;

    public PromptController(PromptService promptService, DTOMapper<LorebooksRecord> lorebookMapper, DTOMapper<EntryRecord> entryMapper) {
        this.promptService = promptService;
        this.lorebookMapper = lorebookMapper;
        this.entryMapper = entryMapper;
    }

    private record PromptDTO(
            EntityDTO[] lorebooks,
            EntityDTO[] activatedEntries,
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
                        lorebookMapper.wrapRecords(
                                newPrompt.lorebooksManager().activeLorebooks().stream()
                                        .map(LorebookImpl.class::cast)
                                        .map(LorebookImpl::getRecord)
                                        .toList()
                        ),
                        entryMapper.wrapRecords(newPrompt.lorebooksManager().activeEntries()),
                        newPrompt.request()
                )
        );
    }
}
