package chechelpo.frplm.frameworks.llm;

import chechelpo.frplm.utils.llm.GeneratorConfig;
import chechelpo.frplm.utils.llm.OpenAIEndpoints;

import java.util.EnumMap;

public abstract class LLM_backend {
    protected EnumMap<OpenAIEndpoints, String> endpoints;

    protected LLM_backend(EnumMap<OpenAIEndpoints, String> endpoints) {
        this.endpoints = endpoints;
    }

    public String generate(String prompt, GeneratorConfig config) {
        return null;
    }
}
