package chechelpo.frplm.domain.character.starting_locations;

import ch.qos.logback.classic.Level;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartingLocationsService extends EntityService<
        StartingLocationsRecord,
        StartingLocationsStore
        > {

    StartingLocationsService(StartingLocationsStore store, EventBus bus) {
        super(store,bus);
        log.setLevel(Level.DEBUG);
    }

    public @NotNull List<LocationsRecord> getStartingLocationsOf(EntityKey<CharactersRecord> key) {
        List<LocationsRecord> list = store.getStartingLocations(key);
        log.debug("Got starting locations of key {} \n {}", key, list);
        return list;
    }

    public @NotNull List<LocationsRecord> startingLocationAt(EntityKey<CharactersRecord> key, int worldID) {
        return store.getStartingLocationAt(key, worldID);
    }
}