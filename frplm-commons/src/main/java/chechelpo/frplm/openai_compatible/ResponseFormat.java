package chechelpo.frplm.openai_compatible;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseFormat(
        @JsonProperty("type")
        @NotNull String type,

        @JsonProperty("json_schema")
        JsonSchema jsonSchema
) {
    public static final String TEXT = "text";
    public static final String JSON_OBJECT = "json_object";
    public static final String JSON_SCHEMA = "json_schema";

    public ResponseFormat {
        Objects.requireNonNull(type, "type must not be null");

        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }

        if (JSON_SCHEMA.equals(type) && jsonSchema == null) {
            throw new IllegalArgumentException(
                    "jsonSchema must be provided when type is json_schema"
            );
        }
    }

    public static @NotNull ResponseFormat text() {
        return new ResponseFormat(TEXT, null);
    }

    public static @NotNull ResponseFormat jsonObject() {
        return new ResponseFormat(JSON_OBJECT, null);
    }

    public static @NotNull ResponseFormat JsonSchema(
            @NotNull String name,
            @NotNull JsonNode schema,
            boolean strict
    ) {
        return new ResponseFormat(
                JSON_SCHEMA,
                new JsonSchema(name, schema, strict)
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JsonSchema(
            @NotNull String name,
            @NotNull JsonNode schema,
            Boolean strict
    ) {
        public JsonSchema {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(schema, "schema must not be null");

            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
        }
    }
}