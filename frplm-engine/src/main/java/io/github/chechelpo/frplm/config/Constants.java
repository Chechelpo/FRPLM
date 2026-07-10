package io.github.chechelpo.frplm.config;

import java.time.Duration;

public final class Constants {
    private Constants() {}
    /** Timeout for LLM APIs */
    public static final Duration DEFAULT_LLM_TIMEOUT = Duration.ofSeconds(270);
}
