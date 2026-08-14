package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.TableField;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

public class WorldFixtures extends EntityFixtures<WorldsRecord, WorldService> {
    public WorldFixtures(WorldService service, EntityFixtureFactory fixtureFactory, String seed) {
        super(service, fixtureFactory, seed);
    }

    @Override
    protected DoActions<WorldsRecord> getFunctionsToAssignForeignFields(EntityDataPayload<WorldsRecord> sample) {
        return DoActions.instantiate(1);
    }

    @Override
    protected Set<TableField<WorldsRecord, ?>> doNotGenerateFields() {
        return Set.of(WORLDS.LOREBOOK_ID, WORLDS.NEXT_CHARACTER_ID, WORLDS.NEXT_LOCATION_ID, WORLDS.NEXT_REGION_ID);
    }
}
