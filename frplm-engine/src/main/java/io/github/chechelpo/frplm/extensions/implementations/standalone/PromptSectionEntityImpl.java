package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.domain.prompts.section.DefaultSections;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;

public class PromptSectionEntityImpl extends StandaloneEntity<PromptSectionRecord> implements PromptSectionEntitySnapshot {
    protected PromptSectionEntityImpl(PromptSectionRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference asReference() {
        return new PromptSectionEntitySnapshot.Reference(record.getPromptId(), record.getSectionId());
    }

    @Override
    public int position() {
        return record.getPosition();
    }

    @Override
    public Type type() {
        return record.getSectionId() == DefaultSections.CHAT_HISTORY.sectionID ? Type.CHAT_HISTORY : Type.CUSTOM;
    }

    @Override
    public ChatCompletionRole role() {
        return ChatCompletionRole.fromWireValue(getRecord().getRole());
    }

    @Override
    public ChatCompletionMessage renderAsCompletionMessage() {
        return new ChatCompletionMessage(role(), null, content());
    }

    @Override
    public PromptSection.InjectAtPosition getInjectionOrder() {
        if (record.getAtDepth() != null)
            return new PromptSection.InjectAtPosition.AtDepth(record.getAtDepth());

        return new PromptSection.InjectAtPosition.Relative(record.getPosition());
    }

    @Override
    public String content() {
        return record.getContent();
    }
}
