package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

public class CharacterFixtures extends EntityFixtures<CharactersRecord, CharacterService> {
    private final WorldFixtures worldFixtures;
    private final LocationFixtures locationFixtures;

    CharacterFixtures(CharacterService service, EntityFixtureFactory fixtureFactory, @NonNull String seed) {
        super(service, fixtureFactory, seed);
        this.worldFixtures = fixtureFactory.worlds(seed + "-worlds");
        this.locationFixtures = fixtureFactory.locations(seed + "-locations");
    }

    @Override
    protected DoActions<CharactersRecord> getFunctionsToAssignForeignFields(
            EntityDataPayload<CharactersRecord> sample
    ) {
        DoActions<CharactersRecord> consumers = DoActions.instantiate(2);

        sample.getAssignment(CHARACTERS.WORLD_ID)
                .ifUnassignedRun(
                        () -> {
                            WorldsRecord world = worldFixtures.addAndCreateTo(EntityDataPayload.empty());
                            sample.set(CHARACTERS.WORLD_ID, world.getId());
                            consumers.add(
                                    payload -> payload.set(CHARACTERS.WORLD_ID, world.getId())
                            );
                        }
                );

        sample.getAssignment(CHARACTERS.STARTING_LOCATION_ID)
                .ifUnassignedRun(
                        () -> {
                            LocationsRecord location = locationFixtures.createOne(
                                    LOCATIONS.WORLD_ID, sample.requireNonNull(CHARACTERS.WORLD_ID)
                            );
                            consumers.add(
                                    payload ->
                                            payload.set(CHARACTERS.WORLD_ID, location.getWorldId())
                                                    .set(CHARACTERS.STARTING_LOCATION_ID, location.getId())
                            );
                        }
                );

        return consumers;
    }

    @Override
    protected Set<TableField<CharactersRecord, ?>> doNotGenerateFields() {
        return Set.of(CHARACTERS.LOREBOOK_ID, CHARACTERS.STARTING_LOCATION_ID);
    }
}
