package chechelpo.frplm.utils.endpoints;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

public enum OpenAIEndpoints {
    CHAT_COMPLETIONS("/api/v1/chat/completions"),
    MODELS("/v1/models")
    ;
    public final String pathTemplate;

    OpenAIEndpoints(String pathTemplate) {
        this.pathTemplate = pathTemplate;
    }

    public @NotNull URI appendTo(String host){
        return URI.create(host + CHAT_COMPLETIONS.pathTemplate);
    }
}