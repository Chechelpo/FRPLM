package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

public class LocationFixtures extends EntityFixtures<LocationsRecord, LocationsService> {

    @Contract(pure = true)
    public LocationFixtures(LocationsService locationsService, @NonNull String seed){
        super(locationsService, seed);
    }

    @Override
    protected Set<TableField<LocationsRecord, ?>> doNotGenerateFields() {
        return Set.of(LOCATIONS.LOREBOOK_ID, LOCATIONS.REGION_ID);
    }
}
