package chechelpo.frplm.domain.sessions.core.utils;

import chechelpo.frplm.domain.character.utils.CharacterEntity;
import chechelpo.frplm.domain.lorebook.utils.Lorebook;
import chechelpo.frplm.domain.world.core.utils.World;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static chechelpo.frplm.jooq.generated.Tables.*;

/** Meant as a cache for an active session, started by frontend */
public final class Session extends Entity<SessionsRecord, SessionRepository> {
    private final World world;
    private final CharacterEntity userCharacter;

    Session(EntityKey<SessionsRecord> key, SessionRepository repository) {
        super(key, repository);

        world = repository.getWorldOfSession(EntityKey.of(
                WORLDS.ID,
                this.get(SESSIONS.WORLD_ID))
        );

        userCharacter = repository.getCharacterOfSession(EntityKey.of(
                CHARACTERS.ID,
                this.get(SESSIONS.USER_PERSONA_ID))
        );
    }
    /** World lorebook + current location lorebook + user lorebook + present characters lorebook + globally active lorebooks*/
    private Lorebook @NotNull [] gatherEligibleLorebooks() {
        ArrayList<Lorebook> eligibleLorebooks = new ArrayList<>(4);
        eligibleLorebooks.add(
                repository.getLorebook(EntityKey.of(LOREBOOKS.ID, world.get(WORLDS.LOREBOOK_ID)))
        );
        eligibleLorebooks.add(
                repository.getLorebook(EntityKey.of(LOREBOOKS.ID, world.get(WORLDS.LOREBOOK_ID)))
        );
        eligibleLorebooks.add(
                repository.getLorebook(EntityKey.of(LOREBOOKS.ID, userCharacter.get(CHARACTERS.LOREBOOK_ID)))
        );
        return eligibleLorebooks.toArray(new Lorebook[0]);
    }

    int currentTick(){
        return this.get(SESSIONS.CURRENT_TICK);
    }
}
