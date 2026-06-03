package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;

@Store
final class MovementStore extends EntityStore<MovementsRecord> {
    MovementStore(@NotNull DSLContext ctx) {
        super(ctx, MOVEMENTS, EntityTypes.Types.MOVEMENTS);
    }
}
