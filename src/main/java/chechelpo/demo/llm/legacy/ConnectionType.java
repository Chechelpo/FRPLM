package chechelpo.demo.llm.legacy;

import chechelpo.demo.llm.backends.KoboldAI;
import chechelpo.demo.llm.backends.LLM_service;

import java.util.function.Supplier;

public enum ConnectionType {
    None(() -> null, null),
    KOBOLD(KoboldAI::new, "/api");

    private final Supplier<LLM_service> factory;
    private final String suffix;

    ConnectionType(Supplier<LLM_service> factory, String suffix) {
        this.factory = factory;
        this.suffix = suffix;
    }

    public LLM_service create() {
        return factory.get();
    }
    public String getSuffix() {
        return suffix;
    }
}

