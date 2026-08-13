package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegionFixtures extends EntityFixtures<RegionRecord, RegionService> {
    private final WorldFixtures worldFixtures;

    public RegionFixtures(RegionService service, EntityFixtureFactory fixtureFactory, @NonNull String seed) {
        super(service, fixtureFactory, seed);
        this.worldFixtures = fixtureFactory.worlds(seed);
    }

    @Override
    protected Set<TableField<RegionRecord, ?>> doNotGenerateFields() {
        return Set.of(REGION.LOREBOOK_ID, REGION.PARENT_REGION_ID);
    }

    @Override
    protected List<Consumer<EntityDataPayload<RegionRecord>>> getFunctionsToAssignForeignFields(EntityDataPayload<RegionRecord> sample) {
        var assignment = sample.getAssignment(REGION.WORLD_ID);
        if (assignment.isAssigned()) return List.of();

        WorldsRecord world = worldFixtures.addAndCreateTo(EntityDataPayload.empty());
        return List.of(
                payload -> payload.set(REGION.WORLD_ID, world.getId())
        );
    }

    public void makeParent(@NonNull RegionRecord parent, @NonNull RegionRecord child){
        assertEquals(parent.getWorldId(), child.getWorldId());
        assertTrue(
                service().update(
                        REGION.PARENT_REGION_ID, parent.getId(),
                        service().keyOf(child)
                ).success()
        );

        child.set(REGION.PARENT_REGION_ID, parent.getId());
    }
}
