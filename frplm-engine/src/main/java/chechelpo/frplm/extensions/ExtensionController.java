package chechelpo.frplm.extensions;

import org.jooq.JSON;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/extensions")
final class ExtensionController {
    private final ExtensionService service;

    ExtensionController(ExtensionService service) {
        this.service = service;
    }


    private record ExtensionDTO(
            String id,
            String displayName,
            String description,
            String configPanelUrl
    ) {}
    @GetMapping
    ResponseEntity<List<ExtensionDTO>> getExtensions() {
        return ResponseEntity.ok(
                service.getConfigurableExtensions().stream()
                        .map(ext -> new ExtensionDTO(
                                ext.extensionId(),
                                ext.displayName(),
                                ext.description(),
                                ext.configPanelUrl()
                        )).toList()
        );
    }

    /* Config : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    @GetMapping("/{extensionId}/config")
    ResponseEntity<JsonNode> getConfig(@PathVariable String extensionId) {
        return ResponseEntity.ok(service.getConfig(extensionId));
    }
    @GetMapping("/{extensionId}/config-panel")
    ResponseEntity<String> getConfigPanel(@PathVariable String extensionId) {
        Optional<String> panel = service.getConfigPanel(extensionId);
        return panel.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PutMapping("/{extensionId}/config")
    ResponseEntity<Boolean> updateConfig(@PathVariable String extensionId, @RequestBody JsonNode config) {
        service.saveConfig(extensionId, config);
        return ResponseEntity.ok(true);
    }

    /* User activated : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */
    @PostMapping("/{extensionId}/config")
    JSON activate(@PathVariable String extensionId, @RequestBody JsonNode order){
        return null;
    }
}