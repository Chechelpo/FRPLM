package chechelpo.frplm.core.engine;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.connection.api_hosts.HostService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.NotInitialized;
import chechelpo.frplm.extensions.ExtensionService;
import chechelpo.frplm.extensions.api.session.SessionPrompt;
import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.utils.MessagePrompt;
import chechelpo.frplm.extensions.implementations.session.SessionContext;
import chechelpo.frplm.extensions.implementations.session.SessionImpl;
import chechelpo.frplm.extensions.implementations.standalone.ConnectionImpl;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import chechelpo.frplm.utils.integrations.T2TClient;
import chechelpo.frplm.utils.prompts.Prompt;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static chechelpo.frplm.jooq.generated.Tables.*;

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
                .orElseThrow(() -> {
                    log.error("No session with id {} found", sessionID);
                    return new EntityNotFound("Could not find session with id " + sessionID, Severity.USER);
                });
    }

    @Contract("_ -> new")
    public @NotNull MessagePrompt getNewPrompt(int sessionID) {
        SessionsRecord sessionsRecord = sessionContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID))
                .orElseThrow(() -> new EntityNotFound("Could not find session with id " + sessionID, Severity.USER));
        SessionImpl session = new SessionImpl(sessionsRecord, standaloneContext, sessionContext);

        SessionPrompt promptTemplate = session.getPrompt()
                .orElseThrow(() -> new EntityNotFound("This session has no prompt", Severity.USER));
        Prompt.Builder promptBuilder = (Prompt.Builder) promptTemplate.getNewMessagePrompt();

        ConnectionSnapshot con = promptTemplate.getAssignedConnection()
                .orElseThrow(() -> new EntityNotFound("This prompt has no connection", Severity.USER));

        extensionService.runPrePromptGeneration(sessionsRecord, promptBuilder);
        MessagePrompt rendered = promptBuilder.render(standaloneContext).build(standaloneContext, con.getModelID());
        log.info("Prompt: {}", rendered.renderedRequest());
        return rendered;
    }

    public @NotNull MessagesRecord generateNewMessage(
            int sessionID,
            ChatCompletionRequest prompt
    ) {
        SessionImpl session = new SessionImpl(findOrThrowSession(sessionID), standaloneContext, sessionContext);
        ConnectionImpl con = (ConnectionImpl) session.getPrompt()
                .orElseThrow(() -> new NotInitialized("This session has no prompt", Severity.EXPECTED))
                .getAssignedConnection().orElseThrow(() -> new NotInitialized("This prompt has no connection", Severity.EXPECTED));

        ChatCompletionResponse response = textToTextClient.generate(prompt, con.getRecord())
                .orElseThrow();
        MessagesRecord generated = sessionContext.messages().createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getRecord().getId())
                        .set(MESSAGES.CONTENT, response.choices().getFirst().message().content())
                        .set(MESSAGES.REQUEST_JSON, OBJECT_MAPPER.writeValueAsString(prompt))
                        .build()
        );
        // The following line is needed cause of the deletion by the response service, otherwise content = null
        generated = sessionContext.messages().find(sessionContext.messages().keyOf(generated)).orElseThrow();

        extensionService.runPostGeneration(sessionContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID)).orElseThrow());
        return generated;
    }

    public @NonNull MessagesRecord regenerate(int sessionID, int tick_num) {
        MessagesRecord previous = sessionContext.messages().find(EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionID)
                .set(MESSAGES.TICK_NUM, tick_num)
                .build()
        ).orElseThrow(() -> {
            log.error("Tried to regenerate a non-existent message \n sessionId: {} \n tick num: {}", sessionID, tick_num);
            return new EntityNotFound("No message with this key", Severity.USER);
        });
        if (previous.getRequestJson() == null)
            throw new IllegalArgumentException("Tried to regenerate a message with no prompt");
        SessionImpl session = new SessionImpl(findOrThrowSession(sessionID), standaloneContext, sessionContext);

        ConnectionImpl con = (ConnectionImpl) session.getPrompt()
                .orElseThrow(() -> new NotInitialized("This session has no prompt", Severity.EXPECTED))
                .getAssignedConnection()
                .orElseThrow(() -> new NotInitialized("This prompt has no connection", Severity.EXPECTED));

        var prompt = OBJECT_MAPPER.readValue(previous.getRequestJson(), ChatCompletionRequest.class);
        ChatCompletionResponse response = textToTextClient.generate(prompt, con.getRecord())
                .orElseThrow();

        extensionService.runPostGeneration(findOrThrowSession(sessionID));

        sessionContext.messages().registerNewResponse(sessionID, tick_num, response.choices().getFirst().message().content());

        return sessionContext.messages().find(
                sessionContext.messages().keyOf(previous)
        ).orElseThrow();
    }
}
