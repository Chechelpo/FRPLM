package chechelpo.frplm.extensions.api.utils;

import chechelpo.frplm.extensions.api.EngineRepository;

public record EngineContext(
        EngineRepository standaloneFactory
) {}
