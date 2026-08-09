package io.github.chechelpo.frplm.core.extras.importer;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.API_BASE;

@RestController()
@RequestMapping(API_BASE + "/import")
final class ImporterController {
    private final DTOMapper<WorldsRecord> worldMapper;
    private final ImporterService importerService;

    ImporterController(DTOMapper<WorldsRecord> worldMapper, ImporterService importerService) {
        this.worldMapper = worldMapper;
        this.importerService = importerService;
    }

    @PostMapping(
            value = "/world",
            consumes = "application/zip"
    )
    public ResponseEntity<EntityDTO> importWorld(InputStream stream) throws IOException {
        return ResponseEntity.ok(
                worldMapper.wrapRecord(importerService.importWorld(stream))
        );
    }
}
