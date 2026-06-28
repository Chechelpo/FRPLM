package chechelpo.frplm.openai_compatible;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public enum ChatCompletionRole {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system")
        ;

        private final String wireValue;

        ChatCompletionRole(String wireValue) {
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
        @Contract(pure = true)
        public static @NotNull ChatCompletionRole fromWireValue(String wireValue) {
            return Arrays.stream(ChatCompletionRole.values())
                    .filter(r -> r.wireValue.equals(wireValue))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid role: " + wireValue));
        }
        @Contract(pure=true)
        public @NotNull ChatCompletionMessage withContent(String content) {
            return new ChatCompletionMessage(this, null,content);
        }
    }