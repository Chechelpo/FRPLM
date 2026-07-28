package io.github.chechelpo.frplm.utils.integrations.model_reasoning_efforts;

import java.util.Map;

final class ChatGPT extends ModelFamilyReasoningTranslator {
    ChatGPT() {
        super(
                "gpt",
                Map.of(),
                Map.ofEntries(
                        Map.entry(
                                "gpt-5.6",
                                new String[]{"none", "low", "medium", "high", "xhigh", "max"}
                        ),
                        Map.entry(
                                "gpt-5.5-pro",
                                new String[]{"medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.5",
                                new String[]{"none", "low", "medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.4-pro",
                                new String[]{"medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.4",
                                new String[]{"none", "low", "medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.3-codex",
                                new String[]{"low", "medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.2-pro",
                                new String[]{"medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.2",
                                new String[]{"none", "low", "medium", "high", "xhigh"}
                        ),
                        Map.entry(
                                "gpt-5.1",
                                new String[]{"none", "low", "medium", "high"}
                        ),
                        Map.entry(
                                "gpt-5-pro",
                                new String[]{"high"}
                        ),
                        Map.entry(
                                "gpt-5",
                                new String[]{"minimal", "low", "medium", "high"}
                        )
                )
        );
    }


}