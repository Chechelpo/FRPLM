package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.domain.character.core.CharacterJSON;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewCharacterOrder;
import io.github.chechelpo.frplm.utils.orders.NewLocationOrder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class LocationMapper extends ABSWireMapper<LocationsRecord, LocationJSON, NewLocationOrder> {
    private final EntityWireMapper<CharactersRecord, CharacterJSON, NewCharacterOrder> characterMapper;
    private final EntityReaders entityReaders;
    private final CharacterService characterService;
    private final EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    LocationMapper(
            ObjectMapper mapper,
            EntityWireMapper<CharactersRecord, CharacterJSON, NewCharacterOrder> characterMapper,
            EntityReaders entityReaders,
            CharacterService characterService,
            EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper
    ) {
        super(mapper, LocationJSON.class, null);

        this.characterMapper = characterMapper;
        this.entityReaders = entityReaders;
        this.characterService = characterService;
        this.lorebookMapper = lorebookMapper;
    }

    @Override
    protected String getZipPath(LocationJSON json) {
        return "locations/%s/%s".formatted(
                json.parent_region_name(),
                json.name()
        );
    }

    @Override
    protected LocationJSON internalRecordFrom(
            @NonNull LocationsRecord record,
            @NonNull ZipBuilder zipBuilder
    ) throws IOException {
        String parentRegionName = null;

        if (record.getRegionId() != null) {
            parentRegionName = entityReaders.regions()
                    .find(
                            EntityKey.<RegionRecord>builder()
                                    .set(REGION.ID, record.getRegionId())
                                    .set(REGION.WORLD_ID, record.getWorldId())
                                    .build()
                    )
                    .orElseThrow(
                            "No region with id "
                                    + record.getRegionId()
                                    + " when exporting parent of location: "
                                    + record.getName(),
                            Severity.SYSTEM
                    )
                    .getName();
        }

        return new LocationJSON(
                record.getName(),
                record.getDescription(),
                parentRegionName,

                record.getLocked(),
                record.getX(),
                record.getY(),
                record.getRadius(),

                fetchLorebook(record, zipBuilder),

                characterService.getMatching(
                        EntityDataPayload.<CharactersRecord>builder()
                                .set(CHARACTERS.WORLD_ID, record.getWorldId())
                                .set(CHARACTERS.STARTING_LOCATION_ID, record.getId())
                                .build()
                        )
                        .stream()
                        .map(characterRecord ->
                                characterMapper.jsonRecordFrom(
                                        characterRecord,
                                        zipBuilder
                                )
                        )
                        .toList()
        );
    }

    @Override
    protected NewLocationOrder internalOrderFrom(@NonNull LocationJSON json) {
        return new NewLocationOrder(
                EntityDataPayload.<LocationsRecord>builder()
                        .set(LOCATIONS.NAME, json.name())
                        .set(LOCATIONS.DESCRIPTION, json.description())

                        .set(LOCATIONS.LOCKED, json.locked())
                        .set(LOCATIONS.X, json.x())
                        .set(LOCATIONS.Y, json.y())
                        .set(LOCATIONS.RADIUS, json.radius())

                        .build(),

                json.parent_region_name(),

                lorebookMapper.orderFrom(json.lorebook()),

                json.charactersHere() == null
                        ? List.of()
                        : json.charactersHere()
                        .stream()
                        .map(characterMapper::orderFrom)
                        .toList()
        );
    }

    LorebookJSON fetchLorebook(
            LocationsRecord fromRecord,
            ZipBuilder builder
    ) throws IOException {
        return lorebookMapper.jsonRecordFrom(
                entityReaders.lorebooks().require(
                        EntityKey.of(
                                LOREBOOKS.ID,
                                fromRecord.getLorebookId()
                        )
                ),
                builder
        );
    }
}
