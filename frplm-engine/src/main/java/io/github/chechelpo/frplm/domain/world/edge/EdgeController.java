package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@RestController
@Component
@RequestMapping(EntityConfigs.EDGES_URL)
final class EdgeController extends EntityController<LocationEdgesRecord, EdgeService> {
    private final DTOMapper<LocationsRecord> locationMapper;

    EdgeController(EdgeService service, DTOMapper<LocationEdgesRecord> mapper, DTOMapper<LocationsRecord> locationMapper) {
        super(EntityConfigs.Types.EDGES, service, mapper);
        this.locationMapper = locationMapper;
    }

    @GetMapping("/{worldId}/{locationId}/neighbours")
    public ResponseEntity<EntityDTO[]> getNeighbours(@PathVariable int locationId, @PathVariable int worldId) {
        return ResponseEntity.ok(
                locationMapper.wrapRecords(
                        service.neighboursOf(EntityKey.<LocationsRecord>builder()
                                .set(LOCATIONS.WORLD_ID, worldId)
                                .set(LOCATIONS.ID, locationId)
                                .build()
                        )
                )
        );
    }
}