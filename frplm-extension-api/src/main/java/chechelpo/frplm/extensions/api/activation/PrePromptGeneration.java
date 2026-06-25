package chechelpo.frplm.extensions.api.activation;


import chechelpo.frplm.extensions.api.session.Session;
import chechelpo.frplm.extensions.api.utils.PromptBuilder;

public interface PrePromptGeneration {
    /**
     * Runs after the engine has assembled the initial prompt draft but before
     * lorebook outlet resolution, entry activation, and provider dispatch.
     *
     * <p>Implementations may mutate the supplied PromptBuilder. They must not
     * retain the Session, PromptBuilder, or snapshot objects after this call.</p>
     */
    void run(Session ofSession, PromptBuilder prompt);
}
