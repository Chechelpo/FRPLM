package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Store
final class CurrentLocationStore extends EntityStore<CurrentLocationsRecord> {
    CurrentLocationStore(@NotNull DSLContext ctx) {
        super(ctx, CURRENT_LOCATIONS, EntityTypes.Types.CURRENT_LOCATIONS);
    }

    public @NotNull List<CurrentLocationsRecord> getAtLocation(
            int sessionID,
            int locationID
    ) {
        return ctx.selectFrom(main_table)
                .where(
                        CURRENT_LOCATIONS.SESSION_ID.eq(sessionID)
                                .and(CURRENT_LOCATIONS.LOCATION_ID.eq(locationID))
                )
                .fetch();
    }

}
