package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegionFixtures extends EntityFixtures<RegionRecord, RegionService> {
    public RegionFixtures(RegionService service, @NonNull String seed) {
        super(service, seed);
    }

    @Override
    protected Set<TableField<RegionRecord, ?>> doNotGenerateFields() {
        return Set.of(REGION.LOREBOOK_ID, REGION.PARENT_REGION_ID);
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
