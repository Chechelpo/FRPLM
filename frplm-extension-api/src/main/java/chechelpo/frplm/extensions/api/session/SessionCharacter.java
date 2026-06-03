package chechelpo.frplm.extensions.api.session;

import chechelpo.frplm.extensions.api.results.MoveResult;
import chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;

public interface SessionCharacter extends CharacterSnapshot {
    boolean isUserCharacter();
    SessionLocation getCurrentLocation();
    MoveResult moveTo(SessionLocation location);
}
