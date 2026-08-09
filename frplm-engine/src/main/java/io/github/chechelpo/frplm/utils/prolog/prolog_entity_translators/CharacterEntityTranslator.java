package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.prolog.arguments.PrologArgumentType;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.CharacterImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

@Component
final class CharacterEntityTranslator implements PrologEntityTranslator {
    private final EntityTranslator entityTranslator;
    private final CharacterService characterService;

    CharacterEntityTranslator(EntityTranslator entityTranslator, CharacterService characterService) {
        this.entityTranslator = entityTranslator;
        this.characterService = characterService;
    }

    @PostConstruct
    void register(){
        entityTranslator.register(PrologArgumentType.CHARACTER, this);
    }

    /** Transforms character name -> Character reference (character: ${id}) */
    @Override
    public @NonNull Optional<String> getIdOfRepresentation(String argumentName) {
        return characterService.getOneMatching(CHARACTERS.NAME, argumentName)
                .ifMoreThanOneThrow()
                .asOptional()
                .map(record -> CharacterImpl.asReference(record).encode());
    }

    /** (character: ${id}) -> character name */
    @Override
    public Optional<String> getQualifiedName(String id) {
        return characterService.find(
                EntityKey.of(
                        CHARACTERS.ID,
                        CharacterSnapshot.Reference.fromString(id).id()
                )
        ).map(CharactersRecord::getName);
    }
}
