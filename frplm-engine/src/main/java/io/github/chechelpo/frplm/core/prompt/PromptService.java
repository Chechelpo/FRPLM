package io.github.chechelpo.frplm.core.prompt;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.prompt.building.PromptBudgetManager;
import io.github.chechelpo.frplm.core.prompt.building.PromptOrchestrator;
import io.github.chechelpo.frplm.core.prompt.building.PromptResult;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContext;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.extensions.ExtensionService;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionContext;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizerService;
import io.github.chechelpo.frplm.extensions.api.session.SessionCharacter;
import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.extensions.api.session.SessionPrompt;
import io.github.chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Component
public class PromptService {
    private final ExtensionService extension;
    private final SessionContext sessionContext;
    private final ExtensionContext standaloneContext;
    private final SessionService sessionService;
    private final TokenizerService tokenizerService;

    public LorebookContext lorebookContext;

    public PromptService(
            ExtensionService extensions,
            ExtensionContext extensionContext,
            SessionContext sessionContext,
            LorebookContext lorebookContext,
            SessionService sessionService,
            TokenizerService tokenizerService
    ){
        this.extension = extensions;
        this.sessionContext = sessionContext;
        this.standaloneContext = extensionContext;
        this.lorebookContext = lorebookContext;
        this.sessionService = sessionService;
        this.tokenizerService = tokenizerService;
    }

    public PromptResult getNewPrompt(int sessionId) {
        SessionImpl session = new SessionImpl(
            sessionService.find(EntityKey.of(SESSIONS.ID, sessionId))
                    .orElseThrow(() -> new EntityNotFound("No session with this id", Severity.SYSTEM)),
                standaloneContext,
                sessionContext
        );
        PromptOrchestrator orchestrator = getPromptOrchestrator(session);

        orchestrator.addLorebook(session.getWorld().lorebook());
        SessionLocation currentLocation = session.getUserCharacter().getCurrentLocation();
        orchestrator.addLorebook(currentLocation.getParentRegion().lorebook());
        orchestrator.addLorebook(currentLocation.lorebook());
        currentLocation.getCharactersHere().stream()
                .map(SessionCharacter::lorebook)
                .forEach(orchestrator::addLorebook);

        extension.runPrePromptGeneration(session.getRecord(), orchestrator);

        return orchestrator.render();
    }

    private @NonNull PromptOrchestrator getPromptOrchestrator(@NonNull SessionImpl session) {
        SessionPrompt sessionPrompt = session.getPrompt()
                .orElseThrow(() -> new NotInitialized("Session has no prompt", Severity.EXPECTED));
        ConnectionSnapshot connection = sessionPrompt.getAssignedConnection()
                .orElseThrow(() -> new NotInitialized("Session has no assigned connection", Severity.EXPECTED));

        return new PromptOrchestrator(
                new PromptBudgetManager(connection.getModelID(), sessionPrompt.getBudgetConfig(), tokenizerService),
                lorebookContext,
                session
        );
    }
}
