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

            @Contract(value = " -> new", pure = true)
            public static String @NotNull [] wireValues(){
                return new String[]{USER.wireValue, ASSISTANT.wireValue, SYSTEM.wireValue};
            }
            public static @NotNull Role fromWireValue(String wireValue) {
                for (Role role : Role.values()) {
                    if (role.wireValue.equals(wireValue))
                        return role;
                }
                throw new IllegalArgumentException("Invalid wire value: " + wireValue);
            }
        }
    }