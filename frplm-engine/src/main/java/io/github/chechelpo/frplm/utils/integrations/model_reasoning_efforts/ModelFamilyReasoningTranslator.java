package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

abstract class ModelFamilyReasoningTranslator {

    private static final Pattern SNAPSHOT_SUFFIX =
            Pattern.compile("-\\d{4}-\\d{2}-\\d{2}$");

    private final String baseModel;
    private final Map<String, String[]> modelMapper;
    private final Map<String, String> reasoningMap;

    protected ModelFamilyReasoningTranslator(
            String baseModel,
            Map<String, String> reasoningMap,
            Map<String, String[]> modelMapper
    ) {
        this.baseModel = normalizeModelId(
                Objects.requireNonNull(baseModel, "baseModel")
        );
        this.reasoningMap = reasoningMap;
        this.modelMapper = Map.copyOf(
                Objects.requireNonNull(modelMapper, "modelMapper")
        );
    }

    public final boolean matches(String modelId) {
        return normalizeModelId(modelId).contains(baseModel);
    }


    public final String getReasoning(String baseEffort, String modelId) {
        Objects.requireNonNull(baseEffort, "baseEffort");

        String normalizedModelId = normalizeModelId(modelId);
        String normalizedBaseEffort = Objects.requireNonNull(
                baseEffort,
                "baseEffort"
        ).trim().toLowerCase(Locale.ROOT);

        String normalizedEffort = reasoningMap.getOrDefault(
                normalizedBaseEffort,
                normalizedBaseEffort
        );

        return modelMapper.entrySet()
                .stream()
                .filter(entry -> normalizedModelId.contains(entry.getKey()))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .flatMap(entry -> Arrays.stream(entry.getValue())
                        .filter(normalizedEffort::equals)
                        .findFirst())
                .orElse(null);
    }

    protected static String normalizeModelId(String modelId) {
        String normalized = Objects.requireNonNull(modelId, "modelId")
                .trim()
                .toLowerCase(Locale.ROOT);

        int separator = normalized.lastIndexOf('/');
        if (separator >= 0) {
            normalized = normalized.substring(separator + 1);
        }

        return SNAPSHOT_SUFFIX.matcher(normalized).replaceFirst("");
    }
}