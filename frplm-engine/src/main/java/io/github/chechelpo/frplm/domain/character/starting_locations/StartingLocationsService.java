package io.github.chechelpo.frplm.domain.character.starting_locations;

import ch.qos.logback.classic.Level;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

@Component
public class StartingLocationsService extends EntityService<
        StartingLocationsRecord,
        StartingLocationsStore
        > {
    private final CharacterService characterService;
    StartingLocationsService(
            StartingLocationsStore store,
            FieldValidator<StartingLocationsRecord> validator,
            CharacterService characterService,
            EventBus bus
    ) {
        super(store, validator, bus);
        log.setLevel(Level.DEBUG);
        this.characterService = characterService;
    }

    public @NotNull List<LocationsRecord> getStartingLocationsOf(CharactersRecord character) {
        return getStartingLocationsOf(characterService.keyOf(character));
    }
    public @NotNull List<LocationsRecord> getStartingLocationsOf(EntityKey<CharactersRecord> key) {
        List<LocationsRecord> list = store.getStartingLocations(key);
        log.debug("Got starting locations of key {} \n {}", key, list);
        return list;
    }

    public @NotNull List<LocationsRecord> startingLocationAt(CharactersRecord character, int worldID) {
        return startingLocationAt(characterService.keyOf(character), worldID);
    }
    public @NotNull List<LocationsRecord> startingLocationAt(EntityKey<CharactersRecord> key, int worldID) {
        return store.getStartingLocationAt(key.require(CHARACTERS.ID), worldID);
    }
}