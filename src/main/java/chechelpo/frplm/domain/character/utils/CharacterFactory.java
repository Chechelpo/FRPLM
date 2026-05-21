package chechelpo.frplm.domain.character.utils;

import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public final class CharacterFactory extends EntityFactory<CharactersRecord, CharacterEntity, CharacterRepository> {
    CharacterFactory(CharacterRepository repository) {
        super(repository);
    }

    @Override
    protected CharacterEntity instantiate(@NotNull EntityKey<CharactersRecord> key) {
        return new CharacterEntity(key, repository);
    }
}
