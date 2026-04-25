package chechelpo.demo.domain.chars.starting_locations;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.jooq.generated.tables.records.StartingLocationsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.demo.jooq.generated.Tables.STARTING_LOCATIONS;

@Component
final class StartingLocationsStore extends ABSEntityStore<StartingLocationsRecord>
{
    public StartingLocationsStore(DSLContext ctx) {
        super(ctx, STARTING_LOCATIONS, EntityTypes.Types.STARTING_LOCATIONS);
    }
}
