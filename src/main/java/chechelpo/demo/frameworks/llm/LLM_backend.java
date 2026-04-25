package chechelpo.demo.frameworks.llm;

import chechelpo.demo.utils.llm.GeneratorConfig;
import chechelpo.demo.utils.llm.OpenAIEndpoints;

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
