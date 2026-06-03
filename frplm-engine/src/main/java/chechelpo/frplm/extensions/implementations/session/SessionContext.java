package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.core.MessageService;
import chechelpo.frplm.domain.sessions.movement.CurrentLocationService;

public record SessionContext(
        CurrentLocationService currentLocations,
        MessageService messages,
        SessionService sessions
) {}
