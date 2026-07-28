package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class DeepSeek extends ModelFamilyReasoningTranslator {

    private static final String[] V4_EFFORTS = {
            "high",
            "max"
    };

    private static final String[] NO_EFFORT_PARAMETER = {};

    DeepSeek() {
        super(
                "deepseek",

                /*
                 * DeepSeek V4 compatibility translations:
                 *
                 * low    -> high
                 * medium -> high
                 * xhigh  -> max    
                 *
                 * high and max pass through unchanged.
                 */
                Map.of(
                        "low", "high",
                        "medium", "high",
                        "xhigh", "max"
                ),

                Map.ofEntries(
                        /*
                         * Current official API models.
                         */
                        Map.entry(
                                "deepseek-v4-pro",
                                V4_EFFORTS
                        ),
                        Map.entry(
                                "deepseek-v4-flash",
                                V4_EFFORTS
                        ),

                        /*
                         * Generic/provider-defined V4 identifiers.
                         */
                        Map.entry(
                                "deepseek-v4-preview",
                                V4_EFFORTS
                        ),
                        Map.entry(
                                "deepseek-v4",
                                V4_EFFORTS
                        ),

                        /*
                         * DeepSeek V3.2 family.
                         *
                         * These support reasoning/thinking behavior, but their
                         * official interfaces did not expose effort levels.
                         */
                        Map.entry(
                                "deepseek-v3.2-speciale",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-v3.2-exp",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-v3.2",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * DeepSeek V3.1 family.
                         */
                        Map.entry(
                                "deepseek-v3.1-terminus",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-v3.1",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Original V3 models.
                         *
                         * These are conventional non-thinking chat models,
                         * even though they have substantial reasoning ability.
                         */
                        Map.entry(
                                "deepseek-v3-0324",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-v3",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Full-size R1 reasoning family.
                         */
                        Map.entry(
                                "deepseek-r1-0528",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1-zero",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Official R1 distilled checkpoints.
                         */
                        Map.entry(
                                "deepseek-r1-distill-qwen-1.5b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1-distill-qwen-7b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1-distill-qwen-14b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1-distill-qwen-32b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1-distill-llama-8b",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "deepseek-r1-distill-llama-70b",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * R1-0528 distilled Qwen3 model.
                         */
                        Map.entry(
                                "deepseek-r1-0528-qwen3-8b",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Historical reasoning preview.
                         */
                        Map.entry(
                                "deepseek-r1-lite-preview",
                                NO_EFFORT_PARAMETER
                        )
                )
        );
    }
}