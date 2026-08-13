package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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
    protected List<Consumer<EntityDataPayload<LocationsRecord>>> getFunctionsToAssignForeignFields(
            EntityDataPayload<LocationsRecord> sample
    ) {
        List<Consumer<EntityDataPayload<LocationsRecord>>> consumers = new ArrayList<>(2);
        sample.getAssignment(LOCATIONS.WORLD_ID)
                .ifUnassignedRun(
                        () -> {
                            WorldsRecord world = worldFixtures.addAndCreateTo(EntityDataPayload.empty());
                            consumers.add(
                                    payload ->
                                            payload.set(LOCATIONS.WORLD_ID, world.getId())
                            );
                        }
                );

        sample.getAssignment(LOCATIONS.REGION_ID)
                .ifUnassignedRun(
                        () -> {
                            RegionRecord region = regionFixtures.addAndCreateTo(REGION.WORLD_ID, sample.requireNonNull(LOCATIONS.WORLD_ID));
                            consumers.add(
                                    payload ->
                                            payload.set(LOCATIONS.REGION_ID, region.getId())
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
