package chechelpo.frplm.domain.prompts.template.utils;

import chechelpo.frplm.domain.lorebook.utils.Lorebook;
import chechelpo.frplm.domain.prompts.section.utils.EntityPromptSection;
import chechelpo.frplm.domain.prompts.template.ReasoningEffort;
import chechelpo.frplm.domain.sessions.messages.utils.ChatMessage;
import chechelpo.frplm.domain.world.location.utils.LocationEntity;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class Prompt extends Entity<PromptTemplateRecord, PromptRepository> {
    private final StringBuilder builder = new StringBuilder();
    private final ChatMessage[] chatHistory;

    Prompt(
            EntityKey<PromptTemplateRecord> key,
            PromptRepository repository
    ) {
        super(key, repository);
        this.chatHistory = null;
    }

    public void generate(
            @NotNull LocationEntity currentLocation,
            @NotNull Lorebook @NotNull [] lorebooks,
            @NotNull ChatMessage @NotNull [] messageHistory
    ){

    }
    public @NotNull List<ChatMessage> getPrompt() {
        return null;
    }
    public @NotNull String getAsPlainText() {
        return null;
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
    }

    public record GenerationParameters(
            float temperature,
            float top_p,
            float frequency_penalty,
            float presence_penalty,
            float repetition_penalty,
            int top_k
    ){}
    public record ConfigurationParameters(
            boolean streaming,
            boolean exclude_reasoning,
            ReasoningEffort reasoning_effort
    ){}
}
