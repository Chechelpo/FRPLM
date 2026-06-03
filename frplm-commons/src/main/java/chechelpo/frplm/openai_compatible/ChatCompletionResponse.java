package chechelpo.frplm.openai_compatible;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletionResponse(
            List<ChatChoice> choices
    ) {
    @Override
    public @NotNull String toString() {
        if (choices == null || choices.isEmpty()) {
            return "[empty chat completion response]";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < choices.size(); i++) {
            ChatChoice choice = choices.get(i);

            builder.append("Choice #").append(i).append('\n');

            if (choice == null) {
                builder.append("  [null choice]\n");
                continue;
            }

            ChatCompletionMessage message = choice.message();

            if (message == null) {
                builder.append("  Message: [null]\n");
            } else {
                builder.append("  Role: ")
                        .append(message.role())
                        .append('\n');

                builder.append("  Content:\n")
                        .append(message.content() == null ? "[null]" : message.content())
                        .append('\n');
            }

            builder.append("  Finish reason: ")
                    .append(choice.finish_reason() == null ? "[null]" : choice.finish_reason())
                    .append('\n');
        }

        return builder.toString();
    }
}