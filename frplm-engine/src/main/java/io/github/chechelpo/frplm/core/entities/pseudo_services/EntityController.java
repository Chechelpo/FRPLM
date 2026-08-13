package io.github.chechelpo.frplm.core.entities.pseudo_services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
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

    protected final S service;
    protected final DTOMapper<R> mapper;
    private final EntityConfigs.Types type;

    protected EntityController(EntityConfigs.Types type,S service, DTOMapper<R> mapper) {
        this.type = type;
        this.mapper = mapper;
        this.log = (Logger) LoggerFactory.getLogger(type + "_Controller");
        log.setLevel(Level.convertAnSLF4JLevel(type.getLoggerLevel()));
        log.trace("Controller {} created", type);

        this.service = service;
        REGISTERED_CONTROLLERS_TYPES.add(type);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DTOs
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private URI locationOf(EntityDTO dto) {
        UriComponentsBuilder builder =
                ServletUriComponentsBuilder.fromCurrentRequestUri();

        dto.key().forEach(builder::queryParam);

        return builder.build().encode().toUri();
    }

    public EntityDTO wrapEntity(R record){
        return mapper.wrapRecord(record);
    }
    public EntityDTO[] wrapEntities(R... records){
        return mapper.wrapRecords(List.of(records));
    }
    protected EntityDTO[] wrapEntities(List<R> records){
        return mapper.wrapRecords(records);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // API
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * @param query a map containing (field == value)
     * @return all records if no query, otherwise the records with the matching key.
     */
    @GetMapping(
            value = QUERY_PATH
    )
    protected ResponseEntity<EntityDTO[]> query(@RequestParam(required = false) Map<String, Object> query) {
        if (query == null) return ResponseEntity.badRequest().build();

        if (query.isEmpty())
            return ResponseEntity.ok(
                    mapper.wrapRecords(service.getAll())
            );
        else return ResponseEntity.ok(
                mapper.wrapRecords(
                        service.getMatching(mapper.getDataFrom(query, DTOMapper.DATA_CONSTRUCTION_MODE.QUERY))
                )
        );
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
                mapper.wrapRecord(
                        service.find(
                                mapper.getKeyFromDTO(keyParams, DTOMapper.KEY_CONSTRUCTION_MODE.FULL_KEY)
                        ).orElseThrow(Severity.USER)
                )
        );
    }

    @PatchMapping(
            value = ENTITY_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    protected ResponseEntity<Boolean> patch(@RequestParam Map<String, Object> keyParams, @RequestBody Map<String, Object> patch) {
        service.update(
                mapper.getKeyFromDTO(keyParams, DTOMapper.KEY_CONSTRUCTION_MODE.FULL_KEY),
                mapper.getDataFrom(patch, DTOMapper.DATA_CONSTRUCTION_MODE.QUERY)
        ).orElseThrow();

        return ResponseEntity.ok(true);
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

        R record = service.createAndGet(mapper.getDataFrom(allParams, DTOMapper.DATA_CONSTRUCTION_MODE.CREATE));
        EntityDTO dto = mapper.wrapRecord(record);
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
                service.delete(mapper.getKeyFromDTO(keyParams, DTOMapper.KEY_CONSTRUCTION_MODE.FULL_KEY))
        );
    }
}
