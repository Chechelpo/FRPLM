package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class StartingLocationsStore extends EntityStore<StartingLocationsRecord>
{
    public StartingLocationsStore(DSLContext ctx) {
        super(ctx, STARTING_LOCATIONS, EntityTypes.Types.STARTING_LOCATIONS);
    }

    public @NotNull List<LocationsRecord> getStartingLocationAt(int characterId, int worldID){
        return ctx.select()
                .from(STARTING_LOCATIONS)
                .join(LOCATIONS)
                .on(
                        STARTING_LOCATIONS.LOCATION_ID.eq(LOCATIONS.ID)
                                .and(STARTING_LOCATIONS.WORLD_ID.eq(LOCATIONS.WORLD_ID))
                )
                .where(
                        STARTING_LOCATIONS.WORLD_ID.eq(worldID)
                        .and(STARTING_LOCATIONS.CHARACTER_ID.eq(characterId))
                ).fetchInto(LocationsRecord.class);
    }

    public @NotNull List<LocationsRecord> getStartingLocations(@NotNull EntityKey<CharactersRecord> key){
        log.debug("getStartingLocations() called with key: {}", key);
        log.debug("key values: {}", key.getValues());
        log.debug("key condition: {}", key.getPkCondition());

        Integer characterId = key.getValue(CHARACTERS.ID);

        log.debug("Resolved characterId from key: {}", characterId);

        int totalStartingLocations = ctx.fetchCount(STARTING_LOCATIONS);

        int startingLocationsForCharacter = ctx.fetchCount(
                STARTING_LOCATIONS,
                STARTING_LOCATIONS.CHARACTER_ID.eq(characterId)
        );

        log.debug("Total STARTING_LOCATIONS rows: {}", totalStartingLocations);
        log.debug(
                "STARTING_LOCATIONS rows for character {}: {}",
                characterId,
                startingLocationsForCharacter
        );

        var rawStartingRows = ctx.selectFrom(STARTING_LOCATIONS)
                .where(STARTING_LOCATIONS.CHARACTER_ID.eq(characterId))
                .fetch();

        log.debug("Raw STARTING_LOCATIONS rows for character {}:", characterId);

        for (StartingLocationsRecord row : rawStartingRows) {
            log.debug(
                    "STARTING_LOCATION row => character_id={}, world_id={}, location_id={}",
                    row.getCharacterId(),
                    row.getWorldId(),
                    row.getLocationId()
            );
        }

        var query = ctx.select(LOCATIONS.fields())
                .from(CHARACTERS)
                .join(STARTING_LOCATIONS)
                .on(
                        STARTING_LOCATIONS.CHARACTER_ID.eq(CHARACTERS.ID)
                                .and(key.getPkCondition())
                )
                .join(LOCATIONS)
                .on(
                        LOCATIONS.ID.eq(STARTING_LOCATIONS.LOCATION_ID)
                                .and(LOCATIONS.WORLD_ID.eq(STARTING_LOCATIONS.WORLD_ID))
                );

        log.debug("Generated SQL:\n{}", query.getSQL());
        log.debug("Bind values: {}", query.getBindValues());

        var result = query.fetchInto(LocationsRecord.class);

        log.debug("Joined LOCATIONS result size: {}", result.size());

        for (LocationsRecord location : result) {
            log.debug(
                    "Location result => id={}, world_id={}, name={}",
                    location.getId(),
                    location.getWorldId(),
                    location.getName()
            );
        }

        return result;
        /*return ctx.select(LOCATIONS.fields())
                .from(CHARACTERS)
                .join(STARTING_LOCATIONS)
                .on(
                        STARTING_LOCATIONS.CHARACTER_ID.eq(CHARACTERS.ID)
                                .and(key.getPkCondition())
                )
                .join(LOCATIONS)
                .on(LOCATIONS.ID.eq(STARTING_LOCATIONS.LOCATION_ID)
                        .and(LOCATIONS.WORLD_ID.eq(STARTING_LOCATIONS.WORLD_ID))
                )
                .fetch().into(LocationsRecord.class);*/
    }
}
