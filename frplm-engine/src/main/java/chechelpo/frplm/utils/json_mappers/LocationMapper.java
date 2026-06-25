package chechelpo.frplm.utils.json_mappers;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.utils.json_mappers.orders.NewLocationOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@Component
public class LocationMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final EdgeService edgeService;
    private final LorebookService lorebookService;
    private final LorebookMapper lorebookExporter;
    private final StartingLocationsService startingLocationsService;
    private final CharacterService characterService;
    private final CharacterMapper characterExporter;

    public LocationMapper(EdgeService edgeService, LorebookService lorebookService, LorebookMapper lorebookExporter, StartingLocationsService startingLocationsService, CharacterService characterService, CharacterMapper characterExporter) {
        this.edgeService = edgeService;
        this.lorebookService = lorebookService;
        this.lorebookExporter = lorebookExporter;
        this.startingLocationsService = startingLocationsService;
        this.characterService = characterService;
        this.characterExporter = characterExporter;
    }

    public record LocationJSON(
            String name,
            JsonNode lorebook,
            List<JsonNode> charactersHere
    ){}

    public JsonNode jsonFrom(@NonNull LocationsRecord record){
        return MAPPER.valueToTree(new LocationJSON(
                record.getName(),
                fetchLorebook(record),
                characterService.getStartingAt(record.getWorldId(), record.getId()).stream()
                        .map(characterExporter::jsonFrom)
                        .toList()
        ));
    }

    JsonNode fetchLorebook(LocationsRecord fromRecord){
        return lorebookExporter.jsonFrom(lorebookService.getLorebookOf(fromRecord));
    }

    public NewLocationOrder orderFrom(JsonNode node){
        LocationJSON json = MAPPER.treeToValue(node, LocationJSON.class);
        return new NewLocationOrder(
                EntityDataPayload.of(LOCATIONS.NAME, json.name),
                lorebookExporter.orderFrom(json.lorebook),
                json.charactersHere.stream()
                        .map(characterExporter::fromJson)
                        .toList()
        );
    }
}
