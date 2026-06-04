package chechelpo.frplm.extensions.api.types;

import chechelpo.frplm.extensions.api.EngineRepository;
import chechelpo.frplm.extensions.api.standalone.Snapshot;
import chechelpo.frplm.extensions.api.utils.ExtensionDBBridge;
import chechelpo.frplm.extensions.api.utils.io;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class ConfigurableExtension extends Extension {
    private final JsonNode defaultConfig;
    private final HashMap<String, FieldConfig> fields = new HashMap<>();
    private ExtensionDBBridge dbBridge;
    private final String resourcePath;

    protected ConfigurableExtension(
            String extensionID,
            String name,
            String description,
            String source,
            String resourcePath,
            JsonNode defaultConfig
    ) {
        super(extensionID, name, description, source);
        this.defaultConfig = defaultConfig;
        this.resourcePath = resourcePath;
    }

    protected final void setFieldConfig(@NotNull String fieldName, @NotNull FieldConfig field) {
        if (!defaultConfig.has(fieldName)) throw new IllegalStateException("Field " + fieldName + " not found");
        fields.put(fieldName, field);
    }

    protected final FieldConfig getFieldConfig(String fieldName) {
        return fields.get(fieldName);
    }

    @Contract(pure = true)
    public final @NotNull @Unmodifiable Map<String, FieldConfig> getFields() {
        return Map.copyOf(fields);
    }

    protected final JsonNode getCurrentConfig(){
        return this.dbBridge.getConfig(this.extensionId());
    }

    public final void setDBBridge(ExtensionDBBridge extensionDBBridge) {
        if (this.dbBridge != null) throw new IllegalStateException("DBBridge already set");
        this.dbBridge = extensionDBBridge;
    }

    protected final void saveConfig(JsonNode config) {
        this.dbBridge.saveConfig(this.extensionId(), config);
    }

    /**
     * URL that the frontend can render as the extension's config panel.
     */
    public @NotNull String configPanelUrl(){
        return resourcePath + "/index.html";
    }
    public @NotNull Optional<io.WebAsset> getAsset(String assetName) {
        return io.getAssetFromClassPath(this.getClass(), resourcePath, assetName);
    }
    protected static @NotNull String loadResourceText(
            @NotNull Class<?> anchor,
            @NotNull String absoluteResourcePath
    ) {
        try (var in = anchor.getResourceAsStream(absoluteResourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + absoluteResourcePath);
            }

            return new String(
                    in.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Could not load resource: " + absoluteResourcePath,
                    e
            );
        }
    }

    @Override
    public final JsonNode defaultConfig() {
        return this.defaultConfig;
    }

    public final void updateConfig(@NotNull JsonNode newConfig) {
        validateFieldConfig(newConfig);
        onConfigChange(newConfig);
        saveConfig(newConfig);
    }
    /**
     * @apiNote fields registered with {@link #setFieldConfig(String, FieldConfig)} are validated in advance.
     * If this function exits cleanly, the new config is saved
     */
    public abstract void onConfigChange(JsonNode newConfig);
    private void validateFieldConfig(@NotNull JsonNode newConfig) {
        for (String name : fields.keySet()) {
            if (!newConfig.has(name)) continue;
            FieldConfig fieldConfig = getFieldConfig(name);
            JsonNode newValue = newConfig.get(name);

            if (newValue.isNull())
                if (!fieldConfig.field.nullable()) throw new IllegalArgumentException("Field " + name + " has null value");
                else continue;


            Optional<String> errorMessage = fieldConfig.field.validate(newValue, getRepository());
            if (errorMessage.isPresent()){
                this.logger().severe("Field " + name + " has invalid value: " + errorMessage.get());
                throw new IllegalArgumentException("Field " + name + " has invalid value: " + errorMessage.get());
            }
        }
    }

    public record FieldConfig(
            @NotNull String label,
            @Nullable String description,
            @NotNull Field field
    ){}

    public sealed interface Field {
        boolean nullable();
        Optional<String> validate(JsonNode value, EngineRepository repository);

        record SnapshotSelection<T extends Snapshot>(
                boolean nullable,
                int minSelections,
                int maxSelections,
                @NotNull Class<T> type
        ) implements Field {
            public SnapshotSelection {
                if (type == null) {
                    throw new IllegalArgumentException("type must not be null");
                }

                if (minSelections < 0) {
                    throw new IllegalArgumentException("minSelections must be >= 0");
                }

                if (maxSelections < minSelections) {
                    throw new IllegalArgumentException("maxSelections must be >= minSelections");
                }
            }

            @Override
            public Optional<String> validate(
                    @NotNull JsonNode value,
                    @NotNull EngineRepository repository
            ) {
                if (!value.isArray()) {
                    return Optional.of("Expected array of snapshot references");
                }

                int count = value.size();

                if (count < minSelections || count > maxSelections) {
                    return Optional.of(
                            "Invalid selection count: " + count
                                    + " [" + minSelections + ", " + maxSelections + "]"
                    );
                }

                for (JsonNode element : value) {
                    if (!element.isString()) {
                        return Optional.of("Non string snapshot reference: " + element);
                    }

                    String reference = element.asString();

                    if (reference.isBlank()) {
                        return Optional.of("Blank snapshot reference");
                    }

                    Optional<?> snapshot = repository.get(type, reference);

                    if (snapshot.isEmpty()) {
                        return Optional.of(
                                "No entity of class "
                                        + type.getSimpleName()
                                        + " with reference "
                                        + reference
                        );
                    }
                }

                return Optional.empty();
            }
        }
        sealed interface PrimitiveField extends Field {
            record IntegerConfig(boolean nullable, int minValue, int maxValue) implements PrimitiveField {
                public IntegerConfig{
                    if (maxValue < minValue) throw new IllegalArgumentException();
                }

                @Override
                public @NotNull Optional<String> validate(@NotNull JsonNode value, EngineRepository repository){
                    if (!value.isInt()) return Optional.of("Non integer field: " + value.asString());
                    int integerValue = value.asInt();
                    if (integerValue < minValue || integerValue > maxValue)
                        return Optional.of("Out of bounds: " + integerValue + " [" + minValue + ", " + maxValue + "]");

                    return Optional.empty();
                }
            }

            record DoubleConfig(boolean nullable, double minValue, double maxValue) implements PrimitiveField {
                public DoubleConfig {
                    if (Double.isNaN(minValue) || Double.isNaN(maxValue)) {
                        throw new IllegalArgumentException("Bounds must not be NaN");
                    }

                    if (maxValue < minValue) {
                        throw new IllegalArgumentException("maxValue must be >= minValue");
                    }
                }

                @Override
                public Optional<String> validate(
                        @NotNull JsonNode value,
                        @NotNull EngineRepository repository
                ) {
                    if (!value.isNumber()) {
                        return Optional.of("Non numeric field: " + value);
                    }

                    double doubleValue = value.asDouble();

                    if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                        return Optional.of("Non finite double field: " + doubleValue);
                    }

                    if (doubleValue < minValue || doubleValue > maxValue) {
                        return Optional.of(
                                "Out of bounds: " + doubleValue
                                        + " [" + minValue + ", " + maxValue + "]"
                        );
                    }

                    return Optional.empty();
                }
            }

            record StringConfig(boolean nullable, int minLength, int maxLength) implements PrimitiveField {
                public StringConfig{
                    if (minLength < 0) throw new IllegalArgumentException();
                    if (maxLength < minLength) throw new IllegalArgumentException();
                }

                @Override
                public Optional<String> validate(JsonNode value, EngineRepository repository) {
                    if (!value.isString()) return Optional.of("Non string field: " + value.asString());
                    String stringValue = value.asString();
                    if (stringValue.length() < minLength || stringValue.length() > maxLength){
                        return Optional.of("Out of bounds string length: " + stringValue.length() + " [" + minLength + ", " + maxLength + "]");
                    }

                    return Optional.empty();
                }
            }

            record BooleanConfig(boolean nullable) implements PrimitiveField {
                @Override
                public Optional<String> validate(JsonNode value, EngineRepository repository) {
                    if (!value.isBoolean()) return Optional.of("Non boolean field: " + value.asString());
                    return Optional.empty();
                }
            }
        }
    }
}