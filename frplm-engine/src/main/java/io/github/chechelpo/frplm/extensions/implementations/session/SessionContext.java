package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.sessions.movement.Movements;

public record SessionContext(
        Movements movements,
        MessageService messages,
        SessionService sessions
) {}
