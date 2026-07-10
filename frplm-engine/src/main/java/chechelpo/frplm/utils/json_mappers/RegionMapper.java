package chechelpo.frplm.utils.json_mappers;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.world.region.RegionService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import chechelpo.frplm.utils.json_mappers.orders.NewRegionOrder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
public class RegionMapper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RegionService regionService;
    private final LorebookMapper lorebookMapper;
    private final LorebookService lorebookService;

    public RegionMapper(RegionService regionService, LorebookMapper lorebookMapper, LorebookService lorebookService) {
        this.regionService = regionService;
        this.lorebookMapper = lorebookMapper;
        this.lorebookService = lorebookService;
    }

    private record RegionJSON(
            String name,
            String parent_name,
            JsonNode lorebook
    ){}

    public JsonNode toJson(RegionRecord regionRecord){
        String parentRegionName = null;
        if (regionRecord.getParentRegionId() != null)
            parentRegionName = regionService.find(EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, regionRecord.getWorldId())
                        .set(REGION.ID, regionRecord.getParentRegionId())
                .build()
            ).orElseThrow(() -> new EntityNotFound("No parent region with id: " + regionRecord.getParentRegionId(), Severity.SYSTEM))
                    .getName();

        return OBJECT_MAPPER.valueToTree( new RegionJSON(
                regionRecord.getName(),
                parentRegionName,
                lorebookMapper.jsonFrom(lorebookService.getLorebookOf(regionRecord))
        ));
    }

    public NewRegionOrder fromJson(JsonNode file){
        RegionJSON regionJSON = OBJECT_MAPPER.treeToValue(file, RegionJSON.class);
        if (regionJSON.lorebook == null)
            throw new IllegalArgumentException("Lorebook is null of region " + regionJSON.name);

        return new NewRegionOrder(
                EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.NAME, regionJSON.name)
                        .build(),
                lorebookMapper.orderFrom(regionJSON.lorebook),
                regionJSON.parent_name
        );
    }
}
