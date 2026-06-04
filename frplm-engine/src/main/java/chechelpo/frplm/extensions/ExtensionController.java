package chechelpo.frplm.extensions;

import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.extensions.api.types.ConfigurableExtension;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jooq.JSON;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/extensions")
final class ExtensionController {
    private final ExtensionService service;

    ExtensionController(ExtensionService service) {
        this.service = service;
    }


    private record ConfigurableExtensionDTO(
            String id,
            String displayName,
            String description,
            Map<String, FieldDTO> configMap
    ){ }
    private record FieldDTO(
            String name,
            String description,
            FieldType kind,
            Object value,
            Object[] possible_values
    ){
        private static @NotNull Map<String, FieldDTO> fromFields(@NotNull Map<String, ConfigurableExtension.FieldConfig> input, ExtensionRepository repo) {
            Map<String, FieldDTO> result = new HashMap<>(input.size());
            for (Map.Entry<String, ConfigurableExtension.FieldConfig> entry : input.entrySet()) {
                result.put(entry.getKey(), fromFieldConfig(entry.getValue(), repo));
            }

            return result;
        }
        private static FieldDTO fromFieldConfig(ConfigurableExtension.FieldConfig input, ExtensionRepository repository){
            return null;
        }
    }
    private enum FieldType{String, Number, Double, Boolean}
    @GetMapping
    ResponseEntity<List<ConfigurableExtensionDTO>> getConfigurableExtensions(){
        return ResponseEntity.ok(
                service.getConfigurableExtensions().stream()
                        .map(ext -> new ConfigurableExtensionDTO(
                                ext.extensionId(),
                                ext.displayName(),
                                ext.description(),
                                null
                        )).toList()
        );
    }

    /* Config : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    @GetMapping("/{extensionId}/config")
    ResponseEntity<JsonNode> getConfig(@PathVariable String extensionId) {
        return ResponseEntity.ok(service.getConfig(extensionId));
    }
    @GetMapping(
            value = "/{extensionId}/config-panel",
            produces = MediaType.TEXT_HTML_VALUE
    )
    ResponseEntity<String> getConfigPanel(@PathVariable String extensionId) {
        Optional<String> panel = service.getConfigPanel(extensionId);
        return panel.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{extensionId}/config-panel/**")
    ResponseEntity<ByteArrayResource> getConfigPanelAsset(
            @PathVariable String extensionId,
            HttpServletRequest request
    ) {
        String prefix = "/extensions/" + extensionId + "/config-panel/";
        String requestPath = request.getRequestURI();

        if (!requestPath.startsWith(prefix)) {
            return ResponseEntity.badRequest().build();
        }

        String assetName = requestPath.substring(prefix.length());

        return service.getConfigPanelAsset(extensionId, assetName)
                .map(asset -> ResponseEntity
                        .ok()
                        .contentType(MediaType.parseMediaType(asset.contentType()))
                        .contentLength(asset.bytes().length)
                        .body(new ByteArrayResource(asset.bytes())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{extensionId}/config")
    ResponseEntity<Boolean> updateConfig(@PathVariable String extensionId, @RequestBody JsonNode config) {
        service.getConfigurableExtensions().stream()
                .filter(ext -> ext.extensionId().equals(extensionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFound("No such extension with id " + extensionId, Severity.USER))
                .updateConfig(config);

        return ResponseEntity.ok(true);
    }

    /* User activated : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    @PostMapping("/{extensionId}/config")
    JSON activate(@PathVariable String extensionId, @RequestBody JsonNode order){
        return null;
    }
}