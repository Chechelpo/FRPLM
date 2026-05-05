package chechelpo.frplm.domain.character.starting_locations;

import ch.qos.logback.classic.Level;
import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class StartingLocationsService extends ABSEntityService<
        StartingLocationsRecord,
        StartingLocationsStore
        > {

    StartingLocationsService(
            StartingLocationsStore store
    ) {
        super(store, EntityTypes.Types.STARTING_LOCATIONS);
        log.setLevel(Level.DEBUG);
    }

    public List<LocationsRecord> getStartingLocationsOf(EntityKey<CharactersRecord> key) {
        List<LocationsRecord> list = store.getStartingLocations(key);
        log.debug("Got starting locations of key {} \n {}", key, list);
        return list;
    }
}