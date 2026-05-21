package chechelpo.frplm.domain.character.utils;

import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;

public final class CharacterEntity extends Entity<CharactersRecord, CharacterRepository> {
    CharacterEntity(EntityKey<CharactersRecord> key, CharacterRepository repository) {
        super(key, repository);
    }
}
