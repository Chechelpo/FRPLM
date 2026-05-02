package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.springframework.stereotype.Component;

@Component
public final class StartingLocationsService extends ABSEntityService<
        StartingLocationsRecord,
        StartingLocationsStore
        > {

    StartingLocationsService(
            StartingLocationsStore store
    ) {
        super(store, EntityTypes.Types.STARTING_LOCATIONS);
    }
}