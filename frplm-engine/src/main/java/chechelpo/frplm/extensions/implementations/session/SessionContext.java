package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.MessageService;
import chechelpo.frplm.domain.sessions.movement.Movements;

public record SessionContext(
        Movements movements,
        MessageService messages,
        SessionService sessions
) {}
