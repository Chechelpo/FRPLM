package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

public class LocationFixtures extends EntityFixtures<LocationsRecord, LocationsService> {
    private WorldFixtures worldFixtures;
    private RegionFixtures regionFixtures;

    @Contract(pure = true)
    public LocationFixtures(LocationsService locationsService, EntityFixtureFactory fixtureFactory, @NonNull String seed){
        super(locationsService, fixtureFactory, seed);
        this.worldFixtures = fixtureFactory.worlds(seed);
        this.regionFixtures = fixtureFactory.regions(seed);
    }

    @Override
    protected DoActions<LocationsRecord> getFunctionsToAssignForeignFields(
            EntityDataPayload<LocationsRecord> sample
    ) {
        DoActions<LocationsRecord> consumers = DoActions.instantiate(2);

        sample.getAssignment(LOCATIONS.WORLD_ID)
                .ifUnassignedRun(
                        () -> {
                            WorldsRecord world = worldFixtures.addAndCreateTo(EntityDataPayload.empty());
                            consumers.add(
                                    payload ->
                                            payload.ifUnassignedSet(LOCATIONS.WORLD_ID, world.getId())
                            );
                        }
                );

        sample.getAssignment(LOCATIONS.REGION_ID)
                .ifUnassignedRun(
                        () -> {
                            RegionRecord region = regionFixtures.createOne(REGION.WORLD_ID, sample.requireNonNull(LOCATIONS.WORLD_ID));
                            consumers.add(
                                    payload ->
                                            payload.ifUnassignedSet(LOCATIONS.REGION_ID, region.getId())
                            );
                        }
                );

        return consumers;
    }


    @Override
    protected Set<TableField<LocationsRecord, ?>> doNotGenerateFields() {
        return Set.of(LOCATIONS.LOREBOOK_ID, LOCATIONS.REGION_ID);
    }
}
