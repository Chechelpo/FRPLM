package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.annotations.Ephemeral;
import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import org.jetbrains.annotations.NotNull;

@Ephemeral
public interface SessionLocation extends LocationSnapshot {
    SessionCharacter @NotNull [] getCharactersHere();
    SessionLocation @NotNull [] getSessionNeighbours();
}
