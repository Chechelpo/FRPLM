package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class ReasoningTranslator {
    private List<ModelFamilyReasoningTranslator> translatorList = List.of(
            new ChatGPT(),
            new GLM(),
            new DeepSeek(),
            new Kimi(),
            new Qwen(),
            new Gemini(),
            new XiaomiMiMo()
    );

    public @Nullable String getReasoning(String baseEffort, String modelId){
        if (modelId == null || modelId.endsWith(":thinking")) return null; //NanoGPT handles it or null
        return translatorList.stream()
                .filter(translator -> translator.matches(modelId))
                .findFirst()
                .map(translator -> translator.getReasoning(baseEffort, modelId))
                .orElse(null);
    }
}
