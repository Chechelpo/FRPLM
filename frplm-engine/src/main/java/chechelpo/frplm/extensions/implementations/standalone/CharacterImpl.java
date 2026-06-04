package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;

public class CharacterImpl extends StandaloneEntity<CharactersRecord> implements CharacterSnapshot {
    public CharacterImpl(CharactersRecord record, ExtensionContext context) {
        super(record, context);
    }

    CharactersRecord getRecord() {
        return record;
    }

    @Override
    public Reference reference() {
        return new CharacterSnapshot.Reference(this.record.getId());
    }

    @Override
    public String getName() {
        return record.getName();
    }
    @Override
    public LocationImpl[] getStartingLocations() {
        EntityKey<CharactersRecord> thisKey = context.characters().keyOf(this.getRecord());

        return context.startingLocations().getStartingLocationsOf(thisKey).stream()
                .map(record -> new LocationImpl(record, this.context))
                .toArray(LocationImpl[]::new);
    }

}
