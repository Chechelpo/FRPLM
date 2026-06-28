package chechelpo.frplm.domain.world.region;

import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.REGIONS_URL;
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
}
