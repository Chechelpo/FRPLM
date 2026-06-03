package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import org.jetbrains.annotations.NotNull;

public interface SessionWorld extends WorldSnapshot {
    boolean isAtLocation(@NotNull SessionCharacter character, SessionLocation location);
    SessionLocation locationOf(@NotNull SessionCharacter character);
}
