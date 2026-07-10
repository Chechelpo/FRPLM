package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;

import java.util.List;

public record PromptContext (
        SessionImpl session,
        List<LorebooksRecord> lorebooks,
        List<ChatMessage> chatHistory
){}
