package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.domain.EntityTypes.REGIONS_URL;
import static chechelpo.frplm.jooq.generated.Tables.REGION;

@RestController
@RequestMapping(REGIONS_URL)
final class RegionController extends EntityController<RegionRecord, RegionService> {
    RegionController(RegionService service) {
        super(service);
    }


    @GetMapping("/childrenOf")
    public ResponseEntity<EntityDTO[]> getChildrenRegion(@RequestParam int regionId, @RequestParam int worldId) {
        RegionRecord parent = service.find(EntityKey.<RegionRecord>builder()
                .set(REGION.ID, regionId)
                .set(REGION.WORLD_ID, worldId)
                .build()
        ).orElseThrow(() -> new EntityNotFound("No region with this id", Severity.SYSTEM));

        return ResponseEntity.ok(
                wrapEntities(
                        service.getDepthOneChildrenOf(parent)
                )
        );
    }

    @GetMapping("/{worldId}/roots")
    public ResponseEntity<EntityDTO[]> getRootRegions(@PathVariable int worldId) {
        return ResponseEntity.ok(
                wrapEntities(
                        service.getRoots(worldId)
                )
        );
    }
}
