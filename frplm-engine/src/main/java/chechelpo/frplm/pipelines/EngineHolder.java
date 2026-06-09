package chechelpo.frplm.pipelines;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.LLMService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.extensions.ExtensionService;
import chechelpo.frplm.extensions.implementations.session.SessionImpl;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
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
import chechelpo.frplm.utils.prompts.PromptEntryPoint;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Component
final class EngineHolder {
    private final FullEngineContext engineContext;
    private final ExtensionContext standaloneContext;
    private final Logger log;
    private final ExtensionService extensionService;

    EngineHolder(
            @NotNull CharacterService characters,
            @NotNull ExtensionService extensionService,
            @NotNull WorldService worlds,
            @NotNull LocationsService locations,
            @NotNull EdgeService edges,

            @NotNull LorebookService lorebooks,
            @NotNull EntryService entries,
            @NotNull KeywordService keywords,

            @NotNull CurrentLocationService currentLocations,
            @NotNull MessageService messages,

            @NotNull OutletService outlets,
            @NotNull TemplateService templates,
            @NotNull SectionService sections,

            @NotNull LLMService llm,
            @NotNull SecretService secrets,

            @NotNull SessionService sessionService,
            ExtensionContext context
    ) {
        this.log = (Logger) LoggerFactory.getLogger("ENGINE");
        log.setLevel(Level.TRACE);
        engineContext = new FullEngineContext(
                characters,

                worlds,
                locations,
                edges,

                lorebooks,
                entries,
                keywords,

                currentLocations,
                messages,
                outlets,

                templates,
                sections,

                llm,
                secrets,
                sessionService
        );
        standaloneContext = context;
        this.extensionService = extensionService;
    }

    private SessionsRecord findOrThrowSession(int sessionID) {
        return engineContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID))
                .orElseThrow(() -> {
                    log.error("No session with id {} found", sessionID);
                    return new EntityNotFound("Could not find session with id " + sessionID, Severity.USER);
                });
    }
    @Contract("_ -> new")
    public @NotNull ChatCompletionRequest getNewPrompt(int sessionID) {
        SessionsRecord record = engineContext.sessions().find(EntityKey.of(SESSIONS.ID, sessionID))
                .orElseThrow();
        return null;
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
                    engineContext
            );
            // The following line is needed cause of the deletion by the response service, otherwise content = null
            generated = engineContext.messages().find(engineContext.messages().keyOf(generated)).orElseThrow();

            extensionService.runPostGeneration(session);
            return generated;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
