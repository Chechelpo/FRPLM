package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EdgesFixtures extends EntityFixtures<LocationEdgesRecord, EdgeService> {
    protected EdgesFixtures(EdgeService service, EntityFixtureFactory fixtureFactory, @NonNull String seed) {
        super(service, fixtureFactory, seed);
    }

    @Override
    protected DoActions<LocationEdgesRecord> getFunctionsToAssignForeignFields(EntityDataPayload<LocationEdgesRecord> sample) {
        return null;
    }

    @Override
    protected Set<TableField<LocationEdgesRecord, ?>> doNotGenerateFields() {
        return Set.of();
    }
}
