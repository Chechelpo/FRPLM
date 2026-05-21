package chechelpo.frplm.domain.sessions.core.utils;

import chechelpo.frplm.domain.character.utils.CharacterEntity;
import chechelpo.frplm.domain.character.utils.CharacterFactory;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.utils.Lorebook;
import chechelpo.frplm.domain.lorebook.utils.LorebookFactory;
import chechelpo.frplm.domain.sessions.core.microservices.SessionService;
import chechelpo.frplm.domain.world.core.utils.World;
import chechelpo.frplm.domain.world.core.utils.WorldFactory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public final class SessionRepository extends EntityRepository<SessionsRecord, SessionService> {
    private final WorldFactory worlds;
    private final CharacterFactory characters;
    private final LorebookFactory lorebooks;
    SessionRepository(SessionService service, WorldFactory worlds, CharacterFactory characterFactory, LorebookFactory lorebooks) {
        super(service);
        this.worlds = worlds;
        this.characters = characterFactory;
        this.lorebooks = lorebooks;
    }

    @NotNull World getWorldOfSession(@NotNull EntityKey<WorldsRecord> worldKey){
        return worlds.create(worldKey);
    }

    @NotNull CharacterEntity getCharacterOfSession(@NotNull EntityKey<CharactersRecord> key){
        return characters.create(key);
    }

    @NotNull Lorebook getLorebook(EntityKey<LorebooksRecord> key){
        return lorebooks.create(key);
    }
}
