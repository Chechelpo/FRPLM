package io.github.chechelpo.frplm.core.entities.pseudo_services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidKey;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.fields.coercers.Coercer;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static io.github.chechelpo.frplm.config.controllers.ControllerPaths.*;

/**
 * CRUD controller for table entities with arbitrary key fields plus field retrieval for frontend display information
 */
public abstract class EntityController<
        R extends TableRecord<R>,
        S extends EntityService<R, ?>
        > {
    private static final EnumSet<EntityConfigs.Types> REGISTERED_CONTROLLERS_TYPES = EnumSet.noneOf(EntityConfigs.Types.class);
    protected final Logger log;

    private final HashMap<String, TableField<R, ?>> to_table_field = new HashMap<>();
    private final HashMap<TableField<R, ?>, String> to_name = new HashMap<>();
    private final HashMap<TableField<R, ?>, Coercer<?>> field_coercers = new HashMap<>();

    protected final S service;
    private final EntityConfigs.Types type;

    protected EntityController(S service) {
        this.type = service.getType();
        //if (REGISTERED_CONTROLLERS_TYPES.contains(type))
        //    throw new IllegalStateException("Duplicate controller for type " + type);
        this.log = (Logger) LoggerFactory.getLogger(type + "_Controller");
        log.setLevel(Level.convertAnSLF4JLevel(type.getLoggerLevel()));
        log.trace("Controller {} created", type);

        this.service = service;
        REGISTERED_CONTROLLERS_TYPES.add(type);
    }

    protected EntityController(S service, boolean isSingleton) {
        this.type = service.getType();
        if (isSingleton && REGISTERED_CONTROLLERS_TYPES.contains(type))
            throw new IllegalStateException("Duplicate controller for type " + type);
        this.log = (Logger) LoggerFactory.getLogger(type + "_Controller");
        log.setLevel(Level.convertAnSLF4JLevel(type.getLoggerLevel()));
        log.trace("Controller {} created", type);

        this.service = service;
        if (isSingleton) REGISTERED_CONTROLLERS_TYPES.add(type);
    }


    void registerPublicField(@NotNull TableField<R, ?> field, @Nullable String name, @Nullable Coercer<?> coercer) {
        if (name == null) return;
        if (to_table_field.containsKey(name))
            throw new IllegalStateException("Duplicate field name " + name);

        if (coercer != null)
            field_coercers.put(field, coercer);

        to_table_field.put(name, field);
        to_name.put(field, name);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Translators
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected EntityKey<R> extractKey(@NotNull Map<String, Object> params) {
        Either<DataFormatter.FormatError<R>, Map<TableField<R, ?>, Object>> coercedParams = DataFormatter.coerceValues(
                params,
                to_table_field,
                field_coercers
        );
        if (coercedParams.isLeft()) {
            log.error("Error when extracting key with params {} \n Message: {}",
                    params,
                    coercedParams.leftOrThrow().getMessage()
            );
            throw new InvalidKey(coercedParams.leftOrThrow().getMessage(), Severity.USER);
        }

        return new EntityKey<>(coercedParams.rightOrThrow(), true);
    }

    protected EntityDataPayload<R> extractPayload(@NotNull Map<String, Object> params) {
        Either<DataFormatter.FormatError<R>, Map<TableField<R, ?>, Object>> coercedParams = DataFormatter.coerceValues(
                params,
                to_table_field,
                field_coercers
        );
        if (coercedParams.isLeft()) {
            log.error("Error when extracting data with params {} \n Message: {}",
                    params,
                    coercedParams.leftOrThrow().getMessage()
            );
            throw new InvalidValue(coercedParams.leftOrThrow().getMessage());
        }


        return new EntityDataPayload<>(coercedParams.rightOrThrow());
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DTOs
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
    public record EntityDTO(
            String type,
            Map<String, Object> key,
            Map<String, Object> payload
    ) {
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

            public EntityDTO build() {
                return new EntityDTO(type, key, payload);
            }
        }
    }

    public final EntityDTO @NotNull [] wrapEntities(@NotNull R... records) {
        EntityDTO[] dtos = new EntityDTO[records.length];
        for (int i = 0; i < records.length; i++)
            dtos[i] = wrapEntity(records[i]);

        return dtos;
    }

    public final <T extends List<R>> EntityDTO @NotNull [] wrapEntities(@NotNull T records) {
        EntityDTO[] dtos = new EntityDTO[records.size()];
        for (int i = 0; i < records.size(); i++)
            dtos[i] = wrapEntity(records.get(i));

        return dtos;
    }
    public final <T extends Result<R>> EntityDTO @NotNull [] wrapEntities(@NotNull T records) {
        EntityDTO[] dtos = new EntityDTO[records.size()];
        for (int i = 0; i < records.size(); i++)
            dtos[i] = wrapEntity(records.get(i));

        return dtos;
    }

    public final @Nullable EntityDTO wrapEntity(@Nullable R record) {
        if (record == null) return null;
        EntityDTO.Builder builder = new EntityDTO.Builder().type(this.type.getEntityType());
        for (Map.Entry<TableField<R, ?>, String> entry : to_name.entrySet()) {
            if (service.isKey(entry.getKey())) {
                builder.setKey(
                        to_name.get(entry.getKey()),
                        record.getValue(entry.getKey())
                );
            } else {
                builder.setPayload(
                        to_name.get(entry.getKey()),
                        record.getValue(entry.getKey())
                );
            }
        }
        EntityDTO entity = builder.build();
        log.debug("Entity {} \n {}", entity.type, entity);
        return entity;
    }

    protected record FieldsDTO(
            String name,
            Coercer<?> presentation
    ) {
    }

    private URI locationOf(EntityDTO dto) {
        UriComponentsBuilder builder =
                ServletUriComponentsBuilder.fromCurrentRequestUri();

        dto.key().forEach(builder::queryParam);

        return builder.build().encode().toUri();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // API
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * @param query a map containing key field -> value
     * @return all records if no query, otherwise the records with the matching key.
     */
    @GetMapping(
            value = QUERY_PATH
    )
    protected ResponseEntity<EntityDTO[]> query(@RequestParam(required = false) Map<String, Object> query) {
        if (query == null) return ResponseEntity.badRequest().build();

        if (query.isEmpty()) return ResponseEntity.ok(wrapEntities(service.getAll()));
        else return ResponseEntity.ok(wrapEntities(service.getMatching(extractKey(query))));
    }

    /**
     * @param keyParams map containing keyField -> value
     * @return an entity matching this key or not found
     */
    @GetMapping(
            value = ENTITY_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<EntityDTO> get(@RequestParam Map<String, Object> keyParams) {
        return ResponseEntity.ok(
                wrapEntity(
                        service.find(extractKey(keyParams)).orElseThrow(Severity.USER)
                )
        );
    }

    @PatchMapping(
            value = ENTITY_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<Boolean> patch(@RequestParam Map<String, Object> keyParams, @RequestBody Map<String, Object> patch) {
        boolean patched = service.update(
                extractKey(keyParams),
                extractPayload(patch)
        );

        return ResponseEntity.ok(patched);
    }

    /**
     * @param initialKey  initial key values
     * @param initialData initial data value
     * @return entity resource location + dto
     */
    @PostMapping(
            value = ENTITY_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<EntityDTO> create(
            @RequestParam Map<String, Object> initialKey,
            @RequestBody(required = false) Map<String, Object> initialData) throws URISyntaxException {
        int paramSize = initialData == null ? initialKey.size() : initialKey.size() + initialData.size();
        HashMap<String, Object> allParams = new HashMap<>(paramSize);
        allParams.putAll(initialKey);
        if (initialData != null) allParams.putAll(initialData);
        log.debug("Creating new entity with params: \n {}", allParams);

        R record = service.createAndGet(extractPayload(allParams));
        EntityDTO dto = wrapEntity(record);
        try {
            return ResponseEntity.created(locationOf(dto)).body(dto);
        } catch (Exception e) {
            log.error("Non terminal exception found when creating entity, responding only with entity body. \n{}"
                    , Arrays.toString(e.getStackTrace()));
            return ResponseEntity.ok(dto);
        }
    }

    @DeleteMapping(value = ENTITY_PATH)
    protected ResponseEntity<Boolean> delete(@RequestParam Map<String, Object> keyParams) {
        return ResponseEntity.ok(
                service.delete(extractKey(keyParams))
        );
    }
}
