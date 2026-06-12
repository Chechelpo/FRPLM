package chechelpo.frplm.pipelines;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.LLMService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.gen.GenService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.NotInitialized;
import chechelpo.frplm.extensions.ExtensionService;
import chechelpo.frplm.extensions.api.session.SessionPrompt;
import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.utils.MessagePrompt;
import chechelpo.frplm.extensions.implementations.session.SessionContext;
import chechelpo.frplm.extensions.implementations.session.SessionImpl;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.LlmConnection;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmGenRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import chechelpo.frplm.utils.generation.GenerationEntryPoint;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.domain.prompts.section.SectionService;
import chechelpo.frplm.domain.prompts.template.TemplateService;
import chechelpo.frplm.domain.sessions.messages.core.MessageService;
import chechelpo.frplm.domain.sessions.movement.CurrentLocationService;
import chechelpo.frplm.domain.world.core.WorldService;
import chechelpo.frplm.utils.generation.OpenAICompatible;
import chechelpo.frplm.utils.prompts.Prompt;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class EngineHolder {
    private final ExtensionContext standaloneContext;
    private final Logger log;
    private final ExtensionService extensionService;
    private final SessionContext sessionContext;

    EngineHolder(
            ExtensionContext context,
            SessionContext sessionContext,
            ExtensionService extensions
    ) {
        this.log = (Logger) LoggerFactory.getLogger("ENGINE");
        log.setLevel(Level.TRACE);
        standaloneContext = context;
        this.sessionContext = sessionContext;
        this.extensionService = extensions;
    }

    private SessionsRecord findOrThrowSession(int sessionID) {
        return sessionContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID))
                .orElseThrow(() -> {
                    log.error("No session with id {} found", sessionID);
                    return new EntityNotFound("Could not find session with id " + sessionID, Severity.USER);
                });
    }

    @Contract("_ -> new")
    public @NotNull ChatCompletionRequest getNewPrompt(int sessionID) {
        SessionsRecord record = sessionContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID))
                .orElseThrow();
        SessionImpl session = new SessionImpl(record, standaloneContext, sessionContext);

        SessionPrompt prompt = session.getPrompt()
                .orElseThrow(() -> new EntityNotFound("This session has no prompt", Severity.USER));
        Prompt.Builder builder = (Prompt.Builder) prompt.getNewMessagePrompt();

        ConnectionSnapshot con = prompt.getAssignedConnection()
                .orElseThrow(() -> new EntityNotFound("This prompt has no connection", Severity.USER));

        MessagePrompt rendered = builder.render(standaloneContext).build(standaloneContext, con.getModelID());
        log.info("Prompt: {}", rendered.renderedRequest());
        return rendered.renderedRequest();
    }

    public @NotNull MessagesRecord generateNewMessage(
            int sessionID,
            Optional<ChatCompletionRequest> prompt
    ) {
        try {
            SessionsRecord session = findOrThrowSession(sessionID);

            MessagesRecord generated = GenerationEntryPoint.generateNonStreamingMessage(
                    prompt.orElse(getNewPrompt(sessionID)),
                    session,
                    standaloneContext,
                    sessionContext
            );
            // The following line is needed cause of the deletion by the response service, otherwise content = null
            generated = sessionContext.messages().find(sessionContext.messages().keyOf(generated)).orElseThrow();

            extensionService.runPostGeneration(session);
            return generated;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MessagesRecord regenerate(int sessionID, int tick_num){
        /*
        MessagesRecord previous = sessionContext.messages().find(EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionID)
                .set(MESSAGES.TICK_NUM, tick_num)
                .build()
        ).orElseThrow(() -> {
            log.error("Tried to regenerate a non-existent message \n sessionId: {} \n tick num: {}", sessionID, tick_num);
            throw new EntityNotFound("No message with this key", Severity.USER);
        });
        if (previous.getRequestJson() == null) throw new IllegalArgumentException("Tried to regenerate a message with no prompt");
        SessionImpl session = new SessionImpl(findOrThrowSession(sessionID), standaloneContext, sessionContext);

        ConnectionSnapshot con = session.getPrompt()
                .orElseThrow(() -> new NotInitialized("This session has no prompt", Severity.EXPECTED))
                .getAssignedConnection()
                .orElseThrow(() -> new NotInitialized("This prompt has no connection", Severity.EXPECTED));

        ChatCompletionResponse response = con.generate(previous.getRequestJson());
        sessionContext.currentLocations().rollbackLocationsTo(sessionID, tick_num-1);

        sessionContext.messages().registerNewResponse(
                EntityKey.<LlmGenRecord>builder()
                        .set(LLM_GEN.SESSION_ID, sessionID)
                        .set(LLM_GEN.TICK_NUM, tick_num)
                        .build()
                ,
                response.choices().getFirst().message().content()
        );

        extensionService.runPostGeneration(findOrThrowSession(sessionID));
        return sessionContext.messages().find(
                sessionContext.messages().keyOf(previous)
        ).orElseThrow();*/
        return null;
    }

    public @NotNull ChatCompletionRequest generateSimple(
            ChatCompletionRequest prompt,
            int connectionID
    ) {
        try {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
