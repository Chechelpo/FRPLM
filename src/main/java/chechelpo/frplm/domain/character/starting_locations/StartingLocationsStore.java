package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;

@Component
final class StartingLocationsStore extends ABSEntityStore<StartingLocationsRecord>
{
    public StartingLocationsStore(DSLContext ctx) {
        super(ctx, STARTING_LOCATIONS, EntityTypes.Types.STARTING_LOCATIONS);
    }
}
