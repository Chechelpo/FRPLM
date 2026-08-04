package io.github.chechelpo.frplm.core.prompt;

public enum PromptPhase {

    /**
     * Collects contextual information from available sources.
     */
    CONTEXT_BUILDING,

    /**
     * Evaluates the collected context and applies activation side effects.
     */
    CONTEXT_PROCESSING,

    /**
     * Finalizes the processed context before prompt rendering.
     */
    PRE_RENDER,

    /**
     * Performs processing after the prompt has been rendered. No more changes are allowed.
     */
    POST_RENDER
}