package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import chechelpo.frplm.jooq.generated.tables.PromptSection;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;

import java.util.List;
import java.util.Optional;

public class PromptSectionImpl extends StandaloneEntity<PromptSectionRecord> implements PromptSectionSnapshot {
    protected PromptSectionImpl(PromptSectionRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference reference() {
        return new PromptSectionSnapshot.Reference(record.getPromptId(), record.getSectionId());
    }

    @Override
    public ChatCompletionRole role() {
        return ChatCompletionRole.fromWireValue(getRecord().getRole());
    }

    @Override
    public ChatCompletionMessage asCompletionMessage() {
        return new ChatCompletionMessage(role(), null, content());
    }

    @Override
    public String content() {
        return record.getContent();
    }
}
