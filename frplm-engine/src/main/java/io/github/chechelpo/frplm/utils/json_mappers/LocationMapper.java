package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewLocationOrder;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
public class LocationMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final EdgeService edgeService;
    private final LorebookService lorebookService;
    private final LorebookMapper lorebookExporter;
    private final StartingLocationsService startingLocationsService;
    private final CharacterService characterService;
    private final CharacterMapper characterExporter;
    private final RegionService regionService;

    public LocationMapper(EdgeService edgeService, LorebookService lorebookService, LorebookMapper lorebookExporter, StartingLocationsService startingLocationsService, CharacterService characterService, CharacterMapper characterExporter, RegionService regionService) {
        this.edgeService = edgeService;
        this.lorebookService = lorebookService;
        this.lorebookExporter = lorebookExporter;
        this.startingLocationsService = startingLocationsService;
        this.characterService = characterService;
        this.characterExporter = characterExporter;
        this.regionService = regionService;
    }

    public record LocationJSON(
            String name,
            String parent_region_name,
            JsonNode lorebook,
            List<JsonNode> charactersHere
    ){}

    public JsonNode jsonFrom(@NonNull LocationsRecord record){
        return MAPPER.valueToTree(getJSONRecord(record));
    }

    @TestOnly
    LocationJSON getJSONRecord(@NonNull LocationsRecord record){
        String parentRegionName = null;
        if (record.getRegionId() != null)
            parentRegionName = regionService.find(EntityKey.<RegionRecord>builder()
                            .set(REGION.ID, record.getRegionId())
                            .set(REGION.WORLD_ID, record.getWorldId())
                            .build()
                    ).orElseThrow(() -> new EntityNotFound("No region with id %s when exporting \n %s".formatted(record.getRegionId(), record), Severity.SYSTEM))
                    .getName();

        return new LocationJSON(
                record.getName(),
                parentRegionName,
                fetchLorebook(record),
                characterService.getStartingAt(record.getWorldId(), record.getId()).stream()
                        .map(characterExporter::jsonFrom)
                        .toList()
        );
    }

    JsonNode fetchLorebook(LocationsRecord fromRecord){
        return lorebookExporter.jsonFrom(lorebookService.getLorebookOf(fromRecord));
    }

    public NewLocationOrder orderFrom(JsonNode node){
        LocationJSON json = MAPPER.treeToValue(node, LocationJSON.class);
        if (json.lorebook == null)
            throw new IllegalArgumentException("Location lorebook of " + json.name + " is null");

        return new NewLocationOrder(
                EntityDataPayload.of(LOCATIONS.NAME, json.name),
                json.parent_region_name,
                lorebookExporter.orderFrom(json.lorebook),
                json.charactersHere == null ? List.of():
                        json.charactersHere.stream()
                                .map(characterExporter::fromJson)
                                .toList()
        );
    }
}
