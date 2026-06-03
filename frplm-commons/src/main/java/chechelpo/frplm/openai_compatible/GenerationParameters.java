package chechelpo.frplm.openai_compatible;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record GenerationParameters(
        float temperature,
        float top_p,
        float frequency_penalty,
        float presence_penalty,
        float repetition_penalty,
        int top_k
) {
    private static final float DEFAULT_TEMPERATURE = 0.7f;
    private static final float DEFAULT_TOP_P = 0.95f;
    private static final float DEFAULT_FREQUENCY_PENALTY = 0.0f;
    private static final float DEFAULT_PRESENCE_PENALTY = 0.0f;
    private static final float DEFAULT_REPETITION_PENALTY = 1.0f;
    private static final int DEFAULT_TOP_K = 40;

    public static final GenerationParameters DEFAULT = new GenerationParameters(
            DEFAULT_TEMPERATURE,
            DEFAULT_TOP_P,
            DEFAULT_FREQUENCY_PENALTY,
            DEFAULT_PRESENCE_PENALTY,
            DEFAULT_REPETITION_PENALTY,
            DEFAULT_TOP_K
    );



    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Builder builder(@NotNull GenerationParameters source) {
        return new Builder()
                .temperature(source.temperature)
                .topP(source.top_p)
                .frequencyPenalty(source.frequency_penalty)
                .presencePenalty(source.presence_penalty)
                .repetitionPenalty(source.repetition_penalty)
                .topK(source.top_k);
    }

    public static final class Builder {
        private float temperature = DEFAULT_TEMPERATURE;
        private float top_p = DEFAULT_TOP_P;
        private float frequency_penalty = DEFAULT_FREQUENCY_PENALTY;
        private float presence_penalty = DEFAULT_PRESENCE_PENALTY;
        private float repetition_penalty = DEFAULT_REPETITION_PENALTY;
        private int top_k = DEFAULT_TOP_K;

        private Builder() {
        }

        public @NotNull Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public @NotNull Builder topP(float topP) {
            this.top_p = topP;
            return this;
        }

        public @NotNull Builder frequencyPenalty(float frequencyPenalty) {
            this.frequency_penalty = frequencyPenalty;
            return this;
        }

        public @NotNull Builder presencePenalty(float presencePenalty) {
            this.presence_penalty = presencePenalty;
            return this;
        }

        public @NotNull Builder repetitionPenalty(float repetitionPenalty) {
            this.repetition_penalty = repetitionPenalty;
            return this;
        }

        public @NotNull Builder topK(int topK) {
            this.top_k = topK;
            return this;
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull GenerationParameters build() {
            return new GenerationParameters(
                    temperature,
                    top_p,
                    frequency_penalty,
                    presence_penalty,
                    repetition_penalty,
                    top_k
            );
        }
    }
}