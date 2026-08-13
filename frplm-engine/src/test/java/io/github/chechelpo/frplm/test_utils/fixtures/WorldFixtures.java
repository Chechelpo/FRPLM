package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.TableField;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

public class WorldFixtures extends EntityFixtures<WorldsRecord, WorldService> {
    public WorldFixtures(WorldService service, String seed) {
        super(service, seed);
    }

    @Override
    protected Set<TableField<WorldsRecord, ?>> doNotGenerateFields() {
        return Set.of(WORLDS.LOREBOOK_ID, WORLDS.NEXT_CHARACTER_ID, WORLDS.NEXT_LOCATION_ID, WORLDS.NEXT_REGION_ID);
    }
}
