package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.extensions.api.session.SessionCharacter;
import chechelpo.frplm.extensions.api.session.SessionLocation;
import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import chechelpo.frplm.extensions.implementations.standalone.CharacterImpl;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.extensions.api.results.MoveResult;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SessionCharacterImpl extends CharacterImpl implements SessionCharacter {
    private final CharactersRecord record;
    private final SessionImpl session;
    private final SessionWorldImpl world;

    SessionCharacterImpl(CharactersRecord record, ExtensionContext standaloneContext, SessionImpl session, SessionWorldImpl world) {
        super(record, standaloneContext);
        this.record = record;
        this.session = session;
        this.world = world;
    }

    public CharactersRecord getRecord() {
        return record;
    }

    public MoveResult moveTo(@NotNull SessionLocationImpl location) {
        return world.move(this, location);
    }

    @Override
    public boolean isUserCharacter() {
        return Objects.equals(session.getUserCharacter().record.getId(), this.record.getId());
    }

    @Contract(" -> new")
    public @NotNull SessionLocationImpl getCurrentLocation(){
        return world.locationOf(this);
    }

    @Override
    public MoveResult moveTo(SessionLocation location) {
        return world.move(this, location);
    }
}
