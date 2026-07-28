package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class GLM extends ModelFamilyReasoningTranslator {

    /*
     * Native/canonical GLM-5.2 reasoning-effort values.
     *
     * The API also accepts compatibility aliases, but those are normalized
     * through reasoningMap before this array is checked.
     */
    private static final String[] GLM_52_EFFORTS = {
            "none",
            "high",
            "max"
    };

    /*
     * Empty means the model may support thinking, but does not expose the
     * reasoning_effort parameter.
     */
    private static final String[] NO_EFFORT_PARAMETER = {};
    
    GLM() {
        super(
                "glm",

                /*
                 * GLM-5.2 compatibility normalization:
                 *
                 * minimal -> none
                 * low     -> high
                 * medium  -> high
                 * xhigh   -> max
                 *
                 * none, high and max pass through unchanged.
                 */
                Map.of(
                        "minimal", "none",
                        "low", "high",
                        "medium", "high",
                        "xhigh", "max"
                ),

                Map.ofEntries(
                        /*
                         * GLM-5 family.
                         */
                        Map.entry(
                                "glm-5.2",
                                GLM_52_EFFORTS
                        ),
                        Map.entry(
                                "glm-5.1",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-5-turbo",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-5",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * GLM-4.7 family.
                         */
                        Map.entry(
                                "glm-4.7-flashx",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-4.7-flash",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-4.7",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * GLM-4.6 family.
                         */
                        Map.entry(
                                "glm-4.6",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * GLM-4.5 family.
                         */
                        Map.entry(
                                "glm-4.5-airx",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-4.5-air",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-4.5-flash",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-4.5-x",
                                NO_EFFORT_PARAMETER
                        ),
                        Map.entry(
                                "glm-4.5",
                                NO_EFFORT_PARAMETER
                        ),

                        /*
                         * Earlier GLM-4 API model.
                         */
                        Map.entry(
                                "glm-4-32b-0414-128k",
                                NO_EFFORT_PARAMETER
                        )
                )
        );
    }
}