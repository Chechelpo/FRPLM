package io.github.chechelpo.frplm.core.engine;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.connection.api_hosts.HostService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.extensions.ExtensionService;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionContext;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ConnectionImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.utils.integrations.T2TClient;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Component
final class EngineHolder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ExtensionContext standaloneContext;
    private final Logger log;
    private final ExtensionService extensionService;
    private final SessionContext sessionContext;
    private final T2TClient textToTextClient;

    EngineHolder(
            ExtensionContext context,
            SessionContext sessionContext,
            ExtensionService extensions,
            HostService hostService) {
        this.log = (Logger) LoggerFactory.getLogger("ENGINE");
        log.setLevel(Level.TRACE);
        standaloneContext = context;
        this.sessionContext = sessionContext;
        this.extensionService = extensions;
        this.textToTextClient = new T2TClient(standaloneContext.secrets(), hostService);
    }

    private SessionsRecord findOrThrowSession(int sessionID) {
        return sessionContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID))
                .orElseThrow(Severity.SYSTEM);
    }


    public @NotNull MessagesRecord generateNewMessage(
            int sessionID,
            ChatCompletionRequest prompt
    ) {
        SessionImpl session = new SessionImpl(findOrThrowSession(sessionID), standaloneContext, sessionContext);
        ConnectionImpl con = getConOrThrow(session);

        ChatCompletionResponse response = textToTextClient.generate(prompt, con.getRecord())
                .orElseThrow();
        MessagesRecord generated = sessionContext.messages().createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                        .set(MESSAGES.SESSION_ID, session.getRecord().getId())
                        .set(MESSAGES.CONTENT, response.choices().getFirst().message().content())
                        .set(MESSAGES.REQUEST_JSON, OBJECT_MAPPER.writeValueAsString(prompt))
                        .build()
        );
        // The following line is needed cause of the deletion by the response service, otherwise content = null
        generated = sessionContext.messages().find(sessionContext.messages().keyOf(generated)).orElseThrow(Severity.SYSTEM);

        extensionService.runPostGeneration(
                sessionContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID)).orElseThrow(Severity.SYSTEM)
        );
        return generated;
    }

    public @NonNull MessagesRecord regenerate(int sessionID, int tick_num) {
        MessagesRecord previous = sessionContext.messages().find(EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionID)
                .set(MESSAGES.TICK_NUM, tick_num)
                .build()
        ).orElseThrow("Tried to regenerate a non-existent message", Severity.USER);

        if (previous.getRequestJson() == null)
            throw new IllegalArgumentException("Tried to regenerate a message with no prompt");
        SessionImpl session = new SessionImpl(findOrThrowSession(sessionID), standaloneContext, sessionContext);

        ConnectionImpl con = getConOrThrow(session);

        var prompt = OBJECT_MAPPER.readValue(previous.getRequestJson(), ChatCompletionRequest.class);
        ChatCompletionResponse response = textToTextClient.generate(prompt, con.getRecord())
                .orElseThrow();

        sessionContext.messages().registerNewResponse(sessionID, tick_num, response.choices().getFirst().message().content());

        extensionService.runPostGeneration(findOrThrowSession(sessionID));

        return sessionContext.messages().find(
                sessionContext.messages().keyOf(previous)
        ).orElseThrow(Severity.SYSTEM);
    }

    private static ConnectionImpl getConOrThrow(SessionImpl session) {
        return (ConnectionImpl) session.getPrompt()
                .orElseThrow(notFound ->
                        new NotInitialized("Session " + session.getName() + " has no assigned prompt: \n" + notFound.toDebugString(), Severity.USER)
                )
                .getAssignedConnection()
                .orElseThrow(notFound ->
                        new NotInitialized(
                                "Couldn't get session " + session.getName() + "prompt's llm connection: \n" + notFound.toDebugString(),
                                Severity.USER
                        )
                );
    }


}
