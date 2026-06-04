package chechelpo.frplm.pipelines.generation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.NotInitialized;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import chechelpo.frplm.pipelines.FullEngineContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static chechelpo.frplm.pipelines.generation.OpenAICompatible.generateNonStreaming;

public final class GenerationEntryPoint {
    static final Logger GENERATIONS_LOGGER = (Logger) LoggerFactory.getLogger("GENERATIONS");
    static{
        GENERATIONS_LOGGER.setLevel(Level.TRACE);
    }
    private GenerationEntryPoint() {}

    public static @NotNull MessagesRecord generateNonStreamingMessage(
            @NotNull ChatCompletionRequest request,
            SessionsRecord session,
            FullEngineContext engine
    ) {
        if (request.configurationParameters().streaming())
            throw new IllegalArgumentException("Streaming is not supported via this function");

        LlmConnectionRecord connection = getLLMConnection(session, engine)
                .orElseThrow(() -> new NotInitialized("This session has no connection", Severity.USER));

        LLMBackend backendType = LLMBackend.get(connection.getHostId());
        URI host = getBackendHost(connection, backendType);

        ChatCompletionResponse response = switch (backendType){
            case NANOGPT, OPENAI_COMPATIBLE -> generateNonStreaming(host, connection, request, engine.secrets());
        };
        GENERATIONS_LOGGER.info(response.toString());

        return engine.messages().createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getId())
                        .set(MESSAGES.CONTENT, response.choices().getFirst().message().content())
                        .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                        .build()
        );
    }

    public static @NotNull ChatCompletionResponse generateNonStreamingResponse(
            @NotNull ChatCompletionRequest request,
            LlmConnectionRecord connection,
            ExtensionContext engine
    )  {
        if (request.configurationParameters().streaming())
            throw new IllegalArgumentException("Streaming is not supported via this function");

        LLMBackend backendType = LLMBackend.get(connection.getHostId());
        URI host = getBackendHost(connection, backendType);

        return switch (backendType){
            case NANOGPT, OPENAI_COMPATIBLE -> generateNonStreaming(host, connection, request, engine.secrets());
        };
    }


    private static @NotNull Optional<LlmConnectionRecord> getLLMConnection(
            SessionsRecord session,
            @NotNull FullEngineContext engine
    ) throws EntityNotFound {
        Optional<PromptTemplateRecord> template = engine.templates().getOf(session);
        if (template.isEmpty()) return Optional.empty();

        return engine.llm().fromTemplate(template.get());
    }

    private static @NotNull URI getBackendHost(LlmConnectionRecord connection, @NotNull LLMBackend backend) {
        if (backend.host != null)
            return backend.host;

        throw new IllegalArgumentException("No backend host found");
    }
}
