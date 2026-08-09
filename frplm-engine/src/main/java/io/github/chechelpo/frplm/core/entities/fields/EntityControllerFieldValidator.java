package io.github.chechelpo.frplm.core.entities.fields;

import io.github.chechelpo.frplm.exceptions.runtime.InvalidDTO;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.*;

/**
 * Class used for centralizing initialization of entities.
 */
public abstract class EntityControllerFieldValidator<R extends TableRecord<R>> extends EntityFieldsValidator<R> implements DTOMapper<R> {
    private final List<DTOField<R, ?>> dtoNames;
    private final EntityConfigs.Types type;

    protected EntityControllerFieldValidator(EntityConfigs.Types type) {
        this.dtoNames = List.copyOf(getDTOStructure());
        this.type = type;
    }

    protected record DTOField<R extends TableRecord<R>, T>(TableField<R, T> field, String name) {
        public static <R extends TableRecord<R>, T> DTOField<R, T> of(TableField<R, T> field, String name) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(name);
            return new DTOField<>(field, name);
        }
    }


    protected abstract List<DTOField<R, ?>> getDTOStructure();

    /**
     * Transport class used for structured communication of entities
     * Structured the following way in a JSON:
     * <pre>
     *     "type" : entity_type,
     *     "key" : {
     *         id[0] : id[0].value,
     *         ...,
     *         id[n] : id[n].value
     *     },
     *     "data" : {
     *         attr[0] : attr[0].value,
     *         ... ,
     *         attr[n] : attr[n].value
     *     }
     * </pre>
     */
    private record RuntimeDTO(
            String type,
            Map<String, Object> key,
            Map<String, Object> payload
    ) implements EntityDTO {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String type;
            private final Map<String, Object> key = new LinkedHashMap<>();
            private final Map<String, Object> payload = new LinkedHashMap<>();

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder setKey(String name, Object value) {
                key.put(name, value);
                return this;
            }

            public Builder setPayload(String name, Object value) {
                payload.put(name, value);
                return this;
            }

            public RuntimeDTO build() {
                return new RuntimeDTO(type, key, payload);
            }
        }
    }

    public EntityDTO wrapRecord(R record) {
        RuntimeDTO.Builder builder = RuntimeDTO.builder();

        builder.type(this.type.getEntityType());

        this.dtoNames.forEach(dto -> {
                    RuntimeDTO.Builder ignored = keys.contains(dto.field()) ?
                            builder.setKey(dto.name(), record.get(dto.field())) :
                            builder.setPayload(dto.name(), record.get(dto.field()));
                }
        );

        return builder.build();
    }


    public EntityKey<R> getKeyFromDTO(Map<String, Object> params, boolean expectFullKey) {
        EntityKey.Builder<R> builder = EntityKey.builder();

        dtoNames.forEach(field -> {
            if (keys.contains(field.field()))
                if (params.containsKey(field.name()))
                    builder.unsafeSet(
                            field.field(),
                            fieldInfoMap.get(field.field).coercer
                                    .coerce(params.get(field.name()))
                                    .rightOrThrow()
                    );
                else if (expectFullKey)
                    throw new InvalidDTO("Expected full key. Key is missing " + field.name());
        });

        return builder.build();
    }

    public EntityDataPayload<R> getDataFrom(Map<String, Object> params, boolean expectKeys) {
        EntityDataPayload.Builder<R> builder = EntityDataPayload.builder();

        this.dtoNames.forEach(
                dtoField -> {
                    if (params.containsKey(dtoField.name())) {
                        if (!expectKeys && keys.contains(dtoField.field()))
                            throw new InvalidDTO("DTO includes key field " + dtoField.field() + " in creationPayload");

                        builder.unsafeSet(
                                dtoField.field(),
                                fieldInfoMap.get(dtoField.field()).coerce(params.get(dtoField.name())).rightOrThrow()
                        );
                    }
                }
        );

        return builder.build();
    }
}
