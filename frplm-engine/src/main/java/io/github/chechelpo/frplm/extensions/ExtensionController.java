package io.github.chechelpo.frplm.extensions;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.types.ConfigurableExtension;
import io.github.chechelpo.frplm.extensions.mapper.ExtensionRepository;
import org.jooq.JSON;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.API_BASE;

@RestController
@RequestMapping(API_BASE + "/extensions")
final class ExtensionController {
    private final ExtensionService service;

    ExtensionController(ExtensionService service) {
        this.service = service;
    }

    private record ConfigurableExtensionDTO(
            String id,
            String displayName,
            String description
    ){}

    @GetMapping
    ResponseEntity<List<ConfigurableExtensionDTO>> getConfigurableExtensions(){
        ExtensionRepository repo = service.getExtensionRepository();
        return ResponseEntity.ok(
                service.getConfigurableExtensions().stream()
                        .map(ext -> new ConfigurableExtensionDTO(
                                ext.extensionId(),
                                ext.displayName(),
                                ext.description()
                        )).toList()
        );
    }

    /* Config : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    @GetMapping("/{extensionId}/config")
    ResponseEntity<JsonNode> getConfig(@PathVariable String extensionId) {
        return ResponseEntity.ok(service.getConfig(extensionId));
    }

    @GetMapping(value = "/{extensionId}/{assetName}", produces = "application/javascript")
    ResponseEntity<ByteArrayResource> getAsset(@PathVariable String extensionId, @PathVariable String assetName) {
        return service.getExtensionAsset(extensionId, "panel.js")
                .map(asset -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/javascript"))
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                        .body(new ByteArrayResource(asset.bytes())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{extensionId}/config", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Boolean> updateConfig(@PathVariable String extensionId, @RequestBody JsonNode config) {
        service.getExtensionOfType(extensionId, ConfigurableExtension.class)
                .orElseThrow(() -> new EntityNotFound("No such extension with id " + extensionId, Severity.USER))
                .replaceConfig(config);

        return ResponseEntity.ok(true);
    }
    @PatchMapping(value = "/{extensionId}/config", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Boolean> patchConfig(@PathVariable String extensionId, @RequestBody JsonNode config){
        service.getExtensionOfType(extensionId, ConfigurableExtension.class)
                .orElseThrow(() -> new EntityNotFound("No such extension with id " + extensionId, Severity.USER))
                .patchConfig(config);

        return ResponseEntity.ok(true);
    }

    /* User activated : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    @PostMapping("/{extensionId}/config")
    JSON activate(@PathVariable String extensionId, @RequestBody JsonNode order){
        return null;
    }
}