package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;

public class CharacterImpl extends StandaloneEntity<CharactersRecord> implements CharacterSnapshot {
    public CharacterImpl(CharactersRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference asReference() {
        return new CharacterSnapshot.Reference(this.record.getId());
    }

    @Override
    public LorebookSnapshot lorebook() {
        return new LorebookImpl(context.lorebooks().getLorebookOf(this.getRecord()), this.context);
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
