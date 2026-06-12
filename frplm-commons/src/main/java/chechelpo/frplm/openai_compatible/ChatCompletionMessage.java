package chechelpo.frplm.openai_compatible;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Standard OpenAI-compatible chat-completion section.
 */
public record ChatCompletionMessage(
        @NotNull ChatCompletionRole role,
        @NotNull String content
) {
    @Contract("_ -> new")
    public static @NotNull ChatCompletionMessage user(@NotNull String content) {
        return new ChatCompletionMessage(ChatCompletionRole.USER, content);
    }

    @Contract("_ -> new")
    public static @NotNull ChatCompletionMessage assistant(@NotNull String content) {
        return new ChatCompletionMessage(ChatCompletionRole.ASSISTANT, content);
    }

    @Contract("_ -> new")
    public static @NotNull ChatCompletionMessage system(@NotNull String content) {
        return new ChatCompletionMessage(ChatCompletionRole.SYSTEM, content);
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