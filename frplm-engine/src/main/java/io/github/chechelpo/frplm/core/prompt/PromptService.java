package io.github.chechelpo.frplm.core.prompt;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.prompt.building.PromptBudgetManager;
import io.github.chechelpo.frplm.core.prompt.building.PromptOrchestrator;
import io.github.chechelpo.frplm.core.prompt.building.PromptResult;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContext;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.extensions.ExtensionService;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionContext;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizerService;
import io.github.chechelpo.frplm.extensions.api.session.SessionPrompt;
import io.github.chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Component
public class PromptService {
    private EnumMap<PromptPhase, List<PromptPipelineSection>> steps = new EnumMap<>(PromptPhase.class);

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
            TokenizerService tokenizerService,
            List<PromptPipelineSection> generationPipeline
    ){
        this.extension = extensions;
        this.sessionContext = sessionContext;
        this.standaloneContext = extensionContext;
        this.lorebookContext = lorebookContext;
        this.sessionService = sessionService;
        this.tokenizerService = tokenizerService;

        generationPipeline.forEach(step ->
                steps.computeIfAbsent(
                        step.requestPhase(),
                        ignored -> new ArrayList<>()
                        ).add(step)
                );
    }

    public PromptResult getNewPrompt(int sessionId) {
        SessionImpl session = new SessionImpl(
            sessionService.find(EntityKey.of(SESSIONS.ID, sessionId))
                    .orElseThrow("Couldn't find session id " + sessionId + " when generating new prompt", Severity.SYSTEM),
                standaloneContext,
                sessionContext
        );

        PromptOrchestrator orchestrator = getPromptOrchestrator(session);

        steps.getOrDefault(PromptPhase.CONTEXT_BUILDING, List.of()).forEach(step -> step.run(orchestrator));
        extension.runPrePromptGeneration(session, orchestrator);
        steps.getOrDefault(PromptPhase.CONTEXT_PROCESSING, List.of()).forEach(step -> step.run(orchestrator));
        steps.getOrDefault(PromptPhase.PRE_RENDER, List.of()).forEach(step -> step.run(orchestrator));

        return orchestrator.render();
    }

    private @NonNull PromptOrchestrator getPromptOrchestrator(@NonNull SessionImpl session) {
        SessionPrompt sessionPrompt = session.getPrompt()
                .orElseThrow(notFound ->
                        new NotInitialized(
                                "Session " + session.getName() + " has no assigned prompt: \n" + notFound.toDebugString(),
                                Severity.USER
                        )
                );
        ConnectionSnapshot connection = sessionPrompt.getAssignedConnection()
                .orElseThrow(notFound ->
                        new NotInitialized(
                                "Session " + session.getName() + " has no assigned connection: \n" + notFound.toDebugString(),
                                Severity.USER
                        )
                );

        return new PromptOrchestrator(
                new PromptBudgetManager(connection.getModelID(), sessionPrompt.getBudgetConfig(), tokenizerService),
                lorebookContext,
                session
        );
    }
}
