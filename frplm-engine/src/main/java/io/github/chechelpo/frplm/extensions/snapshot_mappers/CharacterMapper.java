package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.CharacterImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

final class CharacterMapper extends ReferenceMapper<CharactersRecord, CharacterSnapshot.Reference, CharacterSnapshot> {
    CharacterMapper(EntityReader<CharactersRecord> reader) {
        super(
                CharacterSnapshot.class,
                CharacterSnapshot.Reference::fromString,
                CharacterImpl::new,
                reference -> EntityKey.of(CHARACTERS.ID, reference.id()),
                reader
        );
    }

    @Override
    CharacterSnapshot.Reference getExampleReference() {
        return new CharacterSnapshot.Reference(1);
    }
}
