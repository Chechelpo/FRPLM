package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class Qwen extends ModelFamilyReasoningTranslator {

    /*
     * qwen3.8-max-preview uses a distinct native effort vocabulary.
     */
    private static final String[] QWEN_38_EFFORTS = {
            "low",
            "medium",
            "xhigh"
    };

    /*
     * Standard effort values exposed through Alibaba Model Studio's
     * OpenAI-compatible Responses API.
     */
    private static final String[] RESPONSES_API_EFFORTS = {
            "none",
            "minimal",
            "low",
            "medium",
            "high"
    };

    /*
     * Empty means that the model may support thinking, but does not expose
     * a documented reasoning_effort parameter through the relevant API.
     */
    private static final String[] NO_EFFORT_PARAMETER = {};

    Qwen() {
        super(
                "qwen",

                /*
                 * qwen3.8-max-preview calls its maximum level "xhigh".
                 *
                 * This translation does not affect the regular Responses API
                 * models because xhigh is absent from their supported arrays,
                 * so "max" correctly returns null for those models.
                 */
                Map.of(
                        "max", "xhigh"
                ),

                Map.ofEntries(
                        /*
                         * Qwen 3.8
                         *
                         * Thinking-only. The documented native levels are:
                         * low, medium and xhigh.
                         */
                        Map.entry(
                                "qwen3.8-max-preview",
                                QWEN_38_EFFORTS
                        ),

                        /*
                         * Qwen 3.7
                         */
                        Map.entry(
                                "qwen3.7-max-preview",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3.7-max",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.7-plus",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.7-flash",
                                RESPONSES_API_EFFORTS
                        ),

                        /*
                         * Qwen 3.6
                         */
                        Map.entry(
                                "qwen3.6-max-preview",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3.6-plus",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.6-flash",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.6-35b-a3b",
                                RESPONSES_API_EFFORTS
                        ),

                        /*
                         * Qwen 3.5 commercial models.
                         */
                        Map.entry(
                                "qwen3.5-plus",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.5-flash",
                                RESPONSES_API_EFFORTS
                        ),

                        /*
                         * Qwen 3.5 open-weight models.
                         */
                        Map.entry(
                                "qwen3.5-397b-a17b",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.5-122b-a10b",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.5-35b-a3b",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3.5-27b",
                                RESPONSES_API_EFFORTS
                        ),

                        /*
                         * Qwen 3 Max.
                         *
                         * The stable alias is supported by the Responses API.
                         * The older preview alias is retained separately.
                         */
                        Map.entry(
                                "qwen3-max-preview",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-max",
                                RESPONSES_API_EFFORTS
                        ),

                        /*
                         * Commercial aliases backed by Qwen3-generation
                         * models.
                         */
                        Map.entry(
                                "qwen-plus",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen-flash",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen-turbo",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Qwen 3 Coder commercial models.
                         */
                        Map.entry(
                                "qwen3-coder-plus",
                                RESPONSES_API_EFFORTS
                        ),
                        Map.entry(
                                "qwen3-coder-flash",
                                RESPONSES_API_EFFORTS
                        ),

                        /*
                         * Qwen 3 Coder open-weight models.
                         */
                        Map.entry(
                                "qwen3-coder-next",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-coder-480b-a35b-instruct",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-coder-30b-a3b-instruct",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Original Qwen 3 hybrid-thinking models.
                         *
                         * These use enable_thinking rather than a documented
                         * effort enum in the Chat Completions/DashScope API.
                         */
                        Map.entry(
                                "qwen3-235b-a22b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-32b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-30b-a3b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-14b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-8b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-4b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-1.7b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-0.6b",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Thinking-only Qwen 3 models.
                         */
                        Map.entry(
                                "qwen3-next-80b-a3b-thinking",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-235b-a22b-thinking-2507",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-30b-a3b-thinking-2507",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Explicit non-thinking variants.
                         */
                        Map.entry(
                                "qwen3-next-80b-a3b-instruct",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-235b-a22b-instruct-2507",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "qwen3-30b-a3b-instruct-2507",
                                NO_EFFORT_PARAMETER
                        )
                )
        );
    }
}