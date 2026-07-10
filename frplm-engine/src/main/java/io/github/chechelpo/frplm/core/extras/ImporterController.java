package io.github.chechelpo.frplm.core.extras;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryController;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.world.core.WorldController;
import io.github.chechelpo.frplm.utils.json_mappers.LorebookMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import static io.github.chechelpo.frplm.domain.EntityTypes.API_BASE;

@RestController
@RequestMapping(API_BASE + "/import")
class ImporterController {
    private final EntryController entryController;
    private final EntryService entryService;
    private final LorebookMapper lorebookMapper;
    private final WorldController worldController;
    private final ImporterService importerService;

    ImporterController(
            EntryController entryController,
            EntryService entryService,
            LorebookMapper lorebookMapper,
            WorldController worldController,
            ImporterService importerService
    ) {
        this.entryController = entryController;
        this.entryService = entryService;
        this.lorebookMapper = lorebookMapper;
        this.worldController = worldController;
        this.importerService = importerService;
    }

    @PostMapping(
            value = "/world",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EntityController.EntityDTO> importWorld(@RequestBody JsonNode file) {
        return ResponseEntity.ok(
                worldController.wrapEntity(
                        importerService.importWorld(file)
                )
        );
    }
    @GetMapping(
            value = "/world",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonNode> exportWorld(@RequestParam("worldId") int worldId){
        return ResponseEntity.ok(
                importerService.exportWorld(worldId)
        );
    }
}
