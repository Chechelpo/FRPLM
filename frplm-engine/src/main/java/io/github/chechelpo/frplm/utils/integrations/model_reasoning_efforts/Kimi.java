package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class Kimi extends ModelFamilyReasoningTranslator {

    private static final String[] K3_EFFORTS = {
            "low",
            "high",
            "max"
    };

    /*
     * The model may reason, but does not accept reasoning_effort.
     */
    private static final String[] NO_EFFORT_PARAMETER = {};

    Kimi() {
        super(
                "kimi",

                /*
                 * Application-level normalization to K3's supported values.
                 *
                 * These are compatibility translations implemented by this
                 * library, not additional values accepted by the Kimi API.
                 *
                 * minimal -> low
                 * medium  -> high
                 * xhigh   -> max
                 *
                 * low, high and max pass through unchanged.
                 * none remains unsupported because K3 always reasons.
                 */
                Map.of(
                        "minimal", "low",
                        "medium", "high",
                        "xhigh", "max"
                ),

                Map.ofEntries(
                        /*
                         * K3 always reasons and supports:
                         * low, high and max.
                         */
                        Map.entry(
                                "kimi-k3",
                                K3_EFFORTS
                        ),

                        /*
                         * K2.7 Code always reasons, but does not accept
                         * reasoning_effort or allow thinking to be disabled.
                         */
                        Map.entry(
                                "kimi-k2.7-code-highspeed",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "kimi-k2.7-code",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * K2.6 reasons by default and supports:
                         *
                         * thinking.type = enabled | disabled
                         *
                         * It does not support reasoning_effort.
                         */
                        Map.entry(
                                "kimi-k2.6",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * K2.5 has the same thinking toggle as K2.6,
                         * but does not support preserved thinking or
                         * reasoning_effort.
                         */
                        Map.entry(
                                "kimi-k2.5",
                                NO_EFFORT_PARAMETER
                        )
                )
        );
    }
}