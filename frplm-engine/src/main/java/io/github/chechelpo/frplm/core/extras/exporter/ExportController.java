package io.github.chechelpo.frplm.core.extras.exporter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.API_BASE;

@RestController
@RequestMapping(API_BASE + "/export")
final class ExportController {

    private final ExporterService mapperService;

    ExportController(ExporterService mapperService) {
        this.mapperService = mapperService;
    }

    @GetMapping(
            value = "/{worldId}/world",
            produces = "application/zip"
    )
    public ResponseEntity<StreamingResponseBody> exportWorld(
            @PathVariable int worldId
    ) {
        StreamingResponseBody body = outputStream ->
                mapperService.exportWorld(worldId, outputStream);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"world-" + worldId + ".zip\""
                )
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }
}
