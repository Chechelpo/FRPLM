package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class XiaomiMiMo extends ModelFamilyReasoningTranslator {

    private static final String[] API_EFFORTS = {
            "none",
            "low",
            "medium",
            "high"
    };

    /*
     * Model supports reasoning internally, but reasoning_effort is not an
     * intrinsic model parameter when served through arbitrary local runtimes.
     */
    private static final String[] NO_EFFORT_PARAMETER = {};

    XiaomiMiMo() {
        super(
                "mimo",

                /*
                 * Compatibility normalization:
                 *
                 * minimal -> low
                 * xhigh   -> high
                 * max     -> high
                 *
                 * none, low, medium and high pass through unchanged.
                 */
                Map.of(
                        "minimal", "low",
                        "xhigh", "high",
                        "max", "high"
                ),

                Map.ofEntries(
                        /*
                         * Current Xiaomi MiMo API models.
                         */
                        Map.entry(
                                "mimo-v2.5-pro",
                                API_EFFORTS
                        ),
                        Map.entry(
                                "mimo-v2.5",
                                API_EFFORTS
                        ),

                        /*
                         * Deprecated V2 API identifiers.
                         *
                         * Xiaomi currently forwards these identifiers to
                         * corresponding V2.5 models while preserving request
                         * parameters.
                         */
                        Map.entry(
                                "mimo-v2-pro",
                                API_EFFORTS
                        ),
                        Map.entry(
                                "mimo-v2-omni",
                                API_EFFORTS
                        ),
                        Map.entry(
                                "mimo-v2-flash",
                                API_EFFORTS
                        ),

                        /*
                         * V2.5 open-weight base and optimized checkpoints.
                         *
                         * These checkpoints do not independently define an
                         * OpenAI reasoning_effort parameter. Support depends
                         * on the serving provider or inference runtime.
                         */
                        Map.entry(
                                "mimo-v2.5-pro-fp4-dflash",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-v2.5-pro-base",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-v2.5-dflash",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-v2.5-base",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-v2-flash-base",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Original MiMo-7B language models.
                         */
                        Map.entry(
                                "mimo-7b-rl-0530",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-7b-rl-zero",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-7b-rl",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-7b-sft",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-7b-base",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * MiMo-VL reasoning and SFT checkpoints.
                         */
                        Map.entry(
                                "mimo-vl-7b-rl-2508",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-vl-7b-sft-2508",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-vl-7b-rl",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "mimo-vl-7b-sft",
                                NO_EFFORT_PARAMETER
                        )
                )
        );
    }
}