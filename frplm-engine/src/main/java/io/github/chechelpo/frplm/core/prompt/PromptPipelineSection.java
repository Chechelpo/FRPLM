package io.github.chechelpo.frplm.core.prompt;

import io.github.chechelpo.frplm.core.prompt.building.PromptOrchestrator;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;

public interface PromptPipelineSection {
    PromptPhase requestPhase();
    void run(SessionImpl session, PromptOrchestrator orchestrator);
}
