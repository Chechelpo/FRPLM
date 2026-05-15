package chechelpo.frplm.domain.connection.llm.utils.generationRequest;

import chechelpo.frplm.domain.connection.llm.utils.LLMConnection;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ChatCompletionRequest(
            String model,
            @NotNull List<ChatMessage> messages,

            boolean stream
/*
            //Gen params
            Optional<Float> temperature,
            Optional<Float> top_p,
            Optional<Float> frequency_penalty,
            Optional<Float> presence_penalty,
            Optional<Float> repetition_penalty,
            Optional<Float> top_k,


            //Options
            @JsonProperty("max_tokens")
            int maxTokens,


            float top_a,
            float min_p,
            Optional<Float> tfs*/
            ) {
    public static class Builder {
        private final String model;
        private final List<ChatMessage> messages;

        private boolean stream;
        private int maxTokens = 2000;

        Builder(String model, int approxMessageCount) {
            this.model = model;
            this.messages = new ArrayList<>(approxMessageCount);
        }

        public Builder stream() {
            this.stream = true;
            return this;
        }
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        public Builder setMaxOutputTokens(int maxTokens) {
            if (maxTokens <= 0)
                throw new IllegalArgumentException("MaxTokens must be greater than 0");
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder addMessages(List<ChatMessage> messages) {
            this.messages.addAll(messages);
            return this;
        }
        public Builder addMessage(ChatMessage message) {
            this.messages.add(message);
            return this;
        }


        public ChatCompletionRequest build() {
            return null;
        }
    }
}