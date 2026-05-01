package chechelpo.demo.domain.character.starting_locations;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.records.StartingLocationsRecord;
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