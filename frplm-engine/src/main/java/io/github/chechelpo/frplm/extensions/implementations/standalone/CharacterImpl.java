package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static io.github.chechelpo.frplm.jooq.generated.tables.Locations.LOCATIONS;

public class CharacterImpl extends StandaloneEntity<CharactersRecord> implements CharacterSnapshot {
    public CharacterImpl(CharactersRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference asReference() {
        return asReference(this.record);
    }

    public static Reference asReference(CharactersRecord record) {
        return new CharacterSnapshot.Reference(record.getWorldId(), record.getId());
    }
    @Override
    public LorebookSnapshot lorebook() {
        return new LorebookImpl(context.lorebooks().require(
                EntityKey.of(LOREBOOKS.ID, record.getLorebookId())),
                this.context
        );
    }

    @Override
    public String getDescription() {
        return record.getDescription();
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public Optional<LocationSnapshot> getStartingLocations() {
        return Optional.ofNullable(this.record.getStartingLocationId())
                .map(id -> new LocationImpl(
                        context.locations().require(
                                EntityKey.<LocationsRecord>builder()
                                        .set(LOCATIONS.WORLD_ID, this.record.getWorldId())
                                        .set(LOCATIONS.ID, id)
                                        .build()
                        ),
                        context
                ));
    }

}
