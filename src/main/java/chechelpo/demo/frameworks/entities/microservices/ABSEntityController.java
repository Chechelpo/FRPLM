package chechelpo.demo.frameworks.entities.microservices;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.exceptions.Severity;
import chechelpo.demo.exceptions.types.InvalidID;
import chechelpo.demo.exceptions.types.UnknownField;
import chechelpo.demo.frameworks.entities.data.EntityDataPayload;
import chechelpo.demo.frameworks.entities.data.EntityKey;
import chechelpo.demo.frameworks.entities.fields.format.Format;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import static chechelpo.demo.config.controllers.ControllerPaths.*;

/**
 * CRUD controller for table entities with arbitrary key fields plus field retrieval for frontend display information
 */
public abstract class ABSEntityController<
        R extends TableRecord<R>,
        S extends ABSEntityService<R, ?>
        >
{
    private static final EnumSet<EntityTypes.Types> REGISTERED_CONTROLLERS_TYPES = EnumSet.noneOf(EntityTypes.Types.class);
    protected final Logger log;

    private final HashMap<String, TableField<R,?>> to_table_field = new HashMap<>();
    private final HashMap<TableField<R,?>, String> to_name = new HashMap<>();
    private final HashMap<String, Format<?>> field_format = new HashMap<>();

    protected final S service;
    private final EntityTypes.Types type;

    protected ABSEntityController(
            EntityTypes.Types type,
            S service
    ) {
        if (REGISTERED_CONTROLLERS_TYPES.contains(type))
            throw new IllegalStateException("Duplicate controller for type " + type);
        this.log = (Logger) LoggerFactory.getLogger(type + "_Controller");
        log.setLevel(Level.TRACE);
        log.trace("Controller {} created", type);

        this.type = type;
        this.service = service;
        REGISTERED_CONTROLLERS_TYPES.add(type);
    }

    void registerPublicField(@NotNull TableField<R,?> field, @Nullable String name, @Nullable Format<?> format) {
        if (name == null) return;
        if (to_table_field.containsKey(name))
            throw new IllegalStateException("Duplicate field name " + name);

        if (format != null)
            field_format.put(name,format);

        to_table_field.put(name, field);
        to_name.put(field, name);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Translators
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected EntityKey<R> extractKey(@NotNull Map<String, Object> params){
        EntityKey.Builder<R> builder = EntityKey.builder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!to_table_field.containsKey(entry.getKey())){
                log.error("Unknown field name {}", entry.getKey());
                throw new UnknownField("Unknown field " + entry.getKey());
            }

            TableField<R,?> field = to_table_field.get(entry.getKey());
            if (!service.isKey(field)){
                log.error("Field {} is not a key", entry.getKey());
                throw new InvalidID("Field " + field + " is not a key", Severity.USER);
            }

            builder.set(field, entry.getValue());
        }

        return builder.build();
    }

    protected EntityDataPayload<R> extractPayload(@NotNull Map<String, Object> params){
        HashMap<TableField<R,?>, Object> payload = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!to_table_field.containsKey(entry.getKey())){
                log.error("Unknown field name {}", entry.getKey());
                throw new UnknownField("Unknown field " + entry.getKey());
            }
            payload.put(to_table_field.get(entry.getKey()), entry.getValue());
        }

        return new EntityDataPayload<>(payload);
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
     *
     */
    protected record EntityDTO(
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

    protected EntityDTO[] wrapEntities(@NotNull List<R> records) {
        EntityDTO[] dtos = new EntityDTO[records.size()];
        for (int i = 0; i < records.size(); i++)
            dtos[i] = wrapEntity(records.get(i));

        return dtos;
    }

    protected @Nullable EntityDTO wrapEntity(@Nullable R record){
        if (record == null) return null;
        EntityDTO.Builder builder = new EntityDTO.Builder().type(this.type.getEntityType());
        for (Map.Entry<TableField<R,?>, String> entry : to_name.entrySet()){
            //log.info("{} -> {}", entry.getKey(),entry.getValue());
            if (service.isKey(entry.getKey())){
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
            Format<?> presentation
    ) {}

    private URI locationOf(EntityDTO dto) {
        UriComponentsBuilder builder =
                ServletUriComponentsBuilder.fromCurrentRequestUri();

        dto.key().forEach((dtoName, value) ->
                builder.queryParam(dtoName, value)
        );

        return builder.build().encode().toUri();
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // API
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @GetMapping(value = FIELD_QUERY)
    protected ResponseEntity<Map<String, FieldsDTO>> getFields(){
        log.trace("Checking fields for {}", type);
        Map<String, FieldsDTO> map = HashMap.newHashMap(field_format.size());

        for(Map.Entry<String, Format<?>> entry : field_format.entrySet()){
            map.put(entry.getKey(),
                    new FieldsDTO(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        return ResponseEntity.ok(map);
    }


    /**
     * @param query a map containing FieldName -> (value: Object, operator: op)
     * @return records that match this query
     */
    @PostMapping(
            value = QUERY_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<EntityDTO[]> getAll(@RequestBody(required = false) Map<String, Object> query) {
        if (query == null) {
            log.info("Query {}", (Object) wrapEntities(service.getAll()));
            return ResponseEntity.ok(
                    wrapEntities(service.getAll())
            );
        }
        else{
            return null;
        }
            /*
            return ResponseEntity.ok(
                    wrapEntities(service.query(fields.queryOf(query)))
            );*/
    }

    @GetMapping(
            value = ENTITY_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    private ResponseEntity<EntityDTO> get(@RequestParam Map<String, Object> variables){
        return ResponseEntity.ok(
                wrapEntity(service.getForUser(extractKey(variables)))
        );
    }

    @PatchMapping(
            value = ENTITY_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<Boolean> patch(@RequestParam Map<String, Object> identityParams , @RequestBody Map<String, Object> patch){
        boolean patched = service.update(
                extractKey(identityParams),
                extractPayload(patch)
        );

        return ResponseEntity.ok(patched);
    }

    @PostMapping(
            value = ENTITY_PATH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<EntityDTO> create(
            @RequestParam Map<String, Object> params,
            @RequestBody(required = false) Map<String, Object> body) throws URISyntaxException
    {
        int paramSize = body == null ? params.size() : params.size() + body.size();
        HashMap<String, Object> allParams = new HashMap<>(paramSize);
        allParams.putAll(params);
        if (body != null) allParams.putAll(body);
        log.debug("Creating new entity with params: \n {}", allParams);

        R record = service.createAndGet(extractPayload(allParams));
        EntityDTO dto = wrapEntity(record);
        try {
            return ResponseEntity
                    .created(locationOf(dto))
                    .body(dto
            );
        }catch(Exception e){
            log.error("Non terminal exception found when creating entity, responding only with entity body. \n{}"
                    ,Arrays.toString(e.getStackTrace()));
            return ResponseEntity.ok(dto);
        }
    }


}
