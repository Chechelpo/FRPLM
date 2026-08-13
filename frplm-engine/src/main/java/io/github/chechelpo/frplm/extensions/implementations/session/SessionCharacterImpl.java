package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.CharacterImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LorebookImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.StandaloneEntity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.extensions.api.results.MoveResult;
import io.github.chechelpo.frplm.extensions.api.session.SessionCharacter;
import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

public final class SessionCharacterImpl extends StandaloneEntity<SessionCharactersRecord> implements SessionCharacter {
    private final SessionImpl session;
    private final SessionWorldImpl world;

    SessionCharacterImpl(
            SessionCharactersRecord record,
            ExtensionContext standaloneContext,
            SessionImpl session,
            SessionWorldImpl world
    ) {
        super(record, standaloneContext);
        this.session = session;
        this.world = world;
    }
    SessionCharacterImpl(
            CharactersRecord record,
            ExtensionContext standaloneContext,
            SessionImpl session,
            SessionWorldImpl world
    ) {
        super(
                session.context().sessionCharacters().getOneMatching(
                        EntityDataPayload.<SessionCharactersRecord>builder()
                                .set(SESSION_CHARACTERS.SESSION_ID, session.getRecord().getId())
                                .set(SESSION_CHARACTERS.WORLD_ID, session.getRecord().getWorldId())
                                .set(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, record.getId())
                                .build()
                ).resolve(),
                standaloneContext
        );
        this.session = session;
        this.world = world;
    }

    public MoveResult moveTo(@NotNull SessionLocationImpl location) {
        return world.move(this, location);
    }

    @Override
    public boolean isUserCharacter() {
        return Objects.equals(session.getUserCharacter().record.getId(), this.record.getId());
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public String getDescription() {
        return record.getDescription();
    }

    @Contract(" -> new")
    public @NotNull SessionLocationImpl getCurrentLocation() {
        return world.locationOf(this);
    }

    @Override
    public MoveResult moveTo(SessionLocation location) {
        return world.move(this, location);
    }

    @Contract(" -> new")
    @Override
    public @NonNull LorebookSnapshot sessionLorebook() {
        return new LorebookImpl(
                context.lorebooks().require(EntityKey.of(LOREBOOKS.ID, record.getSessionLorebookId())),
                context
                );
    }

    @Override
    public Optional<CharacterSnapshot> getPermanentCharacter() {
        return Optional.ofNullable(this.record.getPermanentCharacterId())
                .map(characterId -> new CharacterImpl(
                        context.characters().require(
                                EntityKey.<CharactersRecord>builder()
                                        .set(CHARACTERS.WORLD_ID, record.getWorldId())
                                        .set(CHARACTERS.ID, record.getPermanentCharacterId())
                                        .build()
                        ),
                        context
                ));
    }
}
