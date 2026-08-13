package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.sessions.session_characters.SessionCharacterService;

public record SessionContext(
        SessionCharacterService sessionCharacters,
        MessageService messages,
        SessionService sessions
) {}
