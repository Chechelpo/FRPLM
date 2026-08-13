package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public class EdgesFixtures extends EntityFixtures<LocationEdgesRecord, EdgeService> {
    protected EdgesFixtures(EdgeService service, @NonNull String seed) {
        super(service, seed);
    }

    @Override
    protected Set<TableField<LocationEdgesRecord, ?>> doNotGenerateFields() {
        return Set.of();
    }
}
