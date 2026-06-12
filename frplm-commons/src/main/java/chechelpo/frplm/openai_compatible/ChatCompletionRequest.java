package chechelpo.frplm.openai_compatible;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        @JsonProperty("model")
        @NotNull String modelID,

        @NotNull
        List<ChatCompletionMessage> messages,

        @NotNull
        GenerationParameters generationParameters,

        @NotNull
        GenerationConfig configurationParameters,

        @JsonProperty("response_format")
        ResponseFormat responseFormat
) {
    public ChatCompletionRequest {
        Objects.requireNonNull(modelID, "modelID must not be null");
        Objects.requireNonNull(messages, "messages must not be null");
        Objects.requireNonNull(generationParameters, "generationParameters must not be null");
        Objects.requireNonNull(configurationParameters, "configurationParameters must not be null");

        if (modelID.isBlank()) {
            throw new IllegalArgumentException("modelID must not be blank");
        }

        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }

        messages = List.copyOf(messages);
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ChatCompletionRequest")
                .append('\n')
                .append("model: ")
                .append(modelID)
                .append('\n');

        sb.append("message_count: ")
                .append(messages.size())
                .append('\n');

        sb.append("generation_parameters: ")
                .append(generationParameters)
                .append('\n');

        sb.append("configuration_parameters: ")
                .append(configurationParameters)
                .append('\n');

        sb.append("response_format: ")
                .append(responseFormat == null ? "<none>" : responseFormat)
                .append('\n');

        sb.append('\n')
                .append("Messages")
                .append('\n')
                .append("========")
                .append('\n');

        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                sb.append('\n')
                        .append("--------")
                        .append('\n');
            }

            sb.append(messages.get(i).toString());
        }

        return sb.toString();
    }

    public Optional<ResponseFormat> responseFormatOptional() {
        return Optional.ofNullable(responseFormat);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Builder builder(@NotNull String modelID) {
        return new Builder().modelID(modelID);
    }

    @Contract("_ -> new")
    public static @NotNull ChatCompletionRequest getTestMessage(@NotNull String modelID) {
        return ChatCompletionRequest.builder(modelID)
                .appendAsSystem("You are a backend health-check endpoint. Reply with exactly: OK")
                .appendAsUser("Return exactly OK.")
                .generationParameters(
                        GenerationParameters.builder()
                                .temperature(0.0f)
                                .topP(1.0f)
                                .frequencyPenalty(0.0f)
                                .presencePenalty(0.0f)
                                .repetitionPenalty(1.0f)
                                .topK(0)
                                .build()
                )
                .configurationParameters(new GenerationConfig(false, true, 1024,null))
                .build();
    }
    @Contract(value="-> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
    }
    public static final class Builder {
        private String modelID;

        private final List<ChatCompletionMessage> messages = new ArrayList<>();

        private GenerationParameters generationParameters = GenerationParameters.DEFAULT;

        private GenerationConfig configurationParameters = new GenerationConfig(
                false,
                true,
                8192,
                ReasoningEffort.Maximum
        );

        private ResponseFormat responseFormat;

        private Builder() {}

        public List<ChatCompletionMessage> getMessages(){
            return this.messages;
        }
        public ChatCompletionMessage getAt(int index){
            return messages.get(index);
        }

        public @NotNull Builder modelID(@NotNull String modelID) {
            this.modelID = Objects.requireNonNull(modelID, "modelID must not be null");
            return this;
        }

        public @NotNull Builder appendAsSystem(@NotNull String content) {
            messages.add(ChatCompletionMessage.system(content));
            return this;
        }

        public @NotNull Builder appendAsUser(@NotNull String content) {
            messages.add(ChatCompletionMessage.user(content));
            return this;
        }

        public @NotNull Builder append(ChatCompletionMessage message){
            messages.add(message);
            return this;
        }
        public Builder setAt(int index, ChatCompletionMessage message){
            messages.set(index, message);
            return this;
        }

        public Builder insertAt(int depth, ChatCompletionMessage message){
            if (message == null) throw new IllegalArgumentException("Can't insert a null message");

            int index = Math.min(0, messages.size() - depth);
            messages.add(index, message);

            return this;
        }

        public @NotNull Builder appendAll(List<ChatCompletionMessage> messages) {
            this.messages.addAll(Objects.requireNonNull(messages, "messages must not be null"));
            return this;
        }

        public @NotNull Builder assistant(@NotNull String content) {
            messages.add(ChatCompletionMessage.assistant(content));
            return this;
        }

        public @NotNull Builder generationParameters(
                @NotNull GenerationParameters generationParameters
        ) {
            this.generationParameters = Objects.requireNonNull(
                    generationParameters,
                    "generationParameters must not be null"
            );
            return this;
        }

        public @NotNull Builder configurationParameters(
                @NotNull GenerationConfig configurationParameters
        ) {
            this.configurationParameters = Objects.requireNonNull(
                    configurationParameters,
                    "configurationParameters must not be null"
            );
            return this;
        }

        public @NotNull Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public @NotNull Builder jsonObjectResponse() {
            this.responseFormat = ResponseFormat.jsonObject();
            return this;
        }
        public @NotNull Builder jsonSchemaResponse(
                @NotNull String name,
                @NotNull JsonNode schema,
                boolean strict
        ) {
            this.responseFormat = ResponseFormat.JsonSchema(
                    name,
                    schema,
                    strict
            );
            return this;
        }

        public @NotNull Builder textResponse() {
            this.responseFormat = ResponseFormat.text();
            return this;
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull ChatCompletionRequest build() {
            if (modelID == null || modelID.isBlank()) {
                throw new IllegalStateException("modelID must be set before build()");
            }

            if (messages.isEmpty()) {
                throw new IllegalStateException(
                        "At least one chat message must be added before build()"
                );
            }

            return new ChatCompletionRequest(
                    modelID,
                    List.copyOf(messages),
                    generationParameters,
                    configurationParameters,
                    responseFormat
            );
        }
    }
}