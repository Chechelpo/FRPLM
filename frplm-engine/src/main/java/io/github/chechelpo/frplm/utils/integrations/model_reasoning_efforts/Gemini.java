package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class Gemini extends ModelFamilyReasoningTranslator {

    private static final String[] STANDARD_EFFORTS = {
            "minimal",
            "low",
            "medium",
            "high"
    };

    /*
     * Gemini 2.5 Flash and Flash-Lite can disable thinking.
     */
    private static final String[] DISABLEABLE_EFFORTS = {
            "none",
            "minimal",
            "low",
            "medium",
            "high"
    };

    Gemini() {
        super(
                "gemini",

                /*
                 * Gemini has no native xhigh or max level.
                 */
                Map.of(
                        "xhigh", "high",
                        "max", "high"
                ),

                Map.ofEntries(
                        /*
                         * Current Gemini 3 models.
                         */
                        Map.entry(
                                "gemini-3.6-flash",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-3.5-flash-lite",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-3.5-flash",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-3.1-pro-preview-customtools",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-3.1-pro-preview",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-3.1-flash-lite",
                                STANDARD_EFFORTS
                        ),

                        /*
                         * Still-available Gemini 3 aliases/previews.
                         */
                        Map.entry(
                                "gemini-3-flash-preview",
                                STANDARD_EFFORTS
                        ),

                        /*
                         * This identifier now redirects to
                         * gemini-3.1-pro-preview.
                         */
                        Map.entry(
                                "gemini-3-pro-preview",
                                STANDARD_EFFORTS
                        ),

                        /*
                         * Dynamic aliases.
                         *
                         * These can change their underlying model, but all
                         * currently resolve to models using the standard
                         * reasoning-effort vocabulary.
                         */
                        Map.entry(
                                "gemini-flash-latest",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-flash-lite-latest",
                                STANDARD_EFFORTS
                        ),
                        Map.entry(
                                "gemini-pro-latest",
                                STANDARD_EFFORTS
                        ),

                        /*
                         * Gemini 2.5 Pro cannot disable thinking.
                         */
                        Map.entry(
                                "gemini-2.5-pro",
                                STANDARD_EFFORTS
                        ),

                        /*
                         * Gemini 2.5 Flash and Flash-Lite support:
                         *
                         * reasoning_effort = none
                         *
                         * through the OpenAI-compatible endpoint.
                         */
                        Map.entry(
                                "gemini-2.5-flash-lite",
                                DISABLEABLE_EFFORTS
                        ),
                        Map.entry(
                                "gemini-2.5-flash",
                                DISABLEABLE_EFFORTS
                        )
                )
        );
    }
}