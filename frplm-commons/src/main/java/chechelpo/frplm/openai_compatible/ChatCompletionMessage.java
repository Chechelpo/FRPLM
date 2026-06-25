package chechelpo.frplm.openai_compatible;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Standard OpenAI-compatible chat-completion section.
 */
public record ChatCompletionMessage(
        @NotNull ChatCompletionRole role,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @JsonAlias("reasoning_content")
        @Nullable String reasoning,

        @Nullable String content
) {
    @Contract("_ -> new")
    public static @NotNull ChatCompletionMessage user(@NotNull String content) {
        return new ChatCompletionMessage(ChatCompletionRole.USER, null, content);
    }

    @Contract("_ -> new")
    public static @NotNull ChatCompletionMessage assistant(@NotNull String content) {
        return new ChatCompletionMessage(ChatCompletionRole.ASSISTANT, null, content);
    }

    @Contract("_ -> new")
    public static @NotNull ChatCompletionMessage system(@NotNull String content) {
        return new ChatCompletionMessage(ChatCompletionRole.SYSTEM, null, content);
    }


    @Override
    public @NotNull String toString() {
        return """
                [
                Role: %s
                Content:
                %s
                ]
                """.formatted(role, content);
    }
}