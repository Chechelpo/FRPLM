package chechelpo.frplm.domain.connection.llm.utils.generationRequest;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Standard OpenAI-compatible chat-completion message. */
    public record ChatMessage(
            @NotNull Role role,
            @NotNull String content
    ) {
        @Contract("_ -> new")
        public static @NotNull ChatMessage user(@NotNull String content) {
            return new ChatMessage(Role.USER, content);
        }

        @Contract("_ -> new")
        public static @NotNull ChatMessage assistant(@NotNull String content) {
            return new ChatMessage(Role.ASSISTANT, content);
        }

        @Contract("_ -> new")
        public static @NotNull ChatMessage system(@NotNull String content) {
            return new ChatMessage(Role.SYSTEM, content);
        }

        public enum Role {
            USER("user"),
            ASSISTANT("assistant"),
            SYSTEM("system")
            ;

            private final String wireValue;

            Role(String wireValue) {
                this.wireValue = wireValue;
            }

            @JsonValue
            public String wireValue() {
                return wireValue;
            }

            public static String[] wireValues(){
                return new String[]{USER.name(), ASSISTANT.name(), SYSTEM.name()};
            }
        }
    }