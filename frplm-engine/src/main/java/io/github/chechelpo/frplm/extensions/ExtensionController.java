package io.github.chechelpo.frplm.extensions;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import io.github.chechelpo.frplm.extensions.api.types.ConfigurableExtension;
import org.jetbrains.annotations.NotNull;
import org.jooq.JSON;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        private static @NotNull Map<String, FieldDTO> fromFields(
                @NotNull Map<String, ConfigurableExtension.FieldConfig> input,
                ExtensionRepository repo
        ) {
            Map<String, FieldDTO> result = new HashMap<>(input.size());
            for (Map.Entry<String, ConfigurableExtension.FieldConfig> entry : input.entrySet()) {
                result.put(entry.getKey(), fromFieldConfig(entry.getValue(), repo));
            }
            return result;
        }

        private static FieldDTO fromFieldConfig(
                ConfigurableExtension.FieldConfig input,
                ExtensionRepository repository
        ) {
            ConfigurableExtension.Field field = input.field();
            FieldType kind = switch (field) {
                case ConfigurableExtension.Field.PrimitiveField.IntegerConfig ic -> FieldType.Number;
                case ConfigurableExtension.Field.PrimitiveField.DoubleConfig dc -> FieldType.Double;
                case ConfigurableExtension.Field.PrimitiveField.StringConfig sc -> FieldType.String;
                case ConfigurableExtension.Field.PrimitiveField.BooleanConfig bc -> FieldType.Boolean;
                case ConfigurableExtension.Field.SnapshotSelection<?> ss -> FieldType.String;
            };

            Object[] possibleValues = switch (field) {
                case ConfigurableExtension.Field.SnapshotSelection<?> ss ->
                        fetchPossibleValues(ss, repository);
                default -> new Object[0];
            };

            return new FieldDTO(
                    input.label(),
                    input.description(),
                    kind,
                    null, // Current value is fetched dynamically via /config endpoint
                    possibleValues
            );
        }

        private static Object[] fetchPossibleValues(
                ConfigurableExtension.Field.SnapshotSelection<?> ss,
                ExtensionRepository repository
        ) {
            Class<?> type = ss.type();
            if (type == ConnectionSnapshot.class) {
                return repository.getConnections().stream()
                        .map(s -> s.asReference().toString())
                        .toArray();
            }
            if (type == CharacterSnapshot.class) {
                return repository.getCharacters().stream()
                        .map(s -> s.asReference().toString())
                        .toArray();
            }
            if (type == WorldSnapshot.class) {
                return repository.getWorlds().stream()
                        .map(s -> s.asReference().toString())
                        .toArray();
            }
            if (type == PromptSnapshot.class) {
                return repository.getPrompts().stream()
                        .map(s -> s.asReference().toString())
                        .toArray();
            }
            return new Object[0];
        }
    }

    private enum FieldType{String, Number, Double, Boolean}

    @GetMapping
    ResponseEntity<List<ConfigurableExtensionDTO>> getConfigurableExtensions(){
        ExtensionRepository repo = service.getExtensionRepository();
        return ResponseEntity.ok(
                service.getConfigurableExtensions().stream()
                        .map(ext -> new ConfigurableExtensionDTO(
                                ext.extensionId(),
                                ext.displayName(),
                                ext.description(),
                                FieldDTO.fromFields(ext.getFields(), repo)
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