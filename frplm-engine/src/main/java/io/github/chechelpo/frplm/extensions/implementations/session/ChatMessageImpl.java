package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.extensions.implementations.standalone.StandaloneEntity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class ChatMessageImpl extends StandaloneEntity<MessagesRecord> implements ChatMessage {
    private final MessagesRecord record;
    private final SessionWorldImpl world;

    public ChatMessageImpl(MessagesRecord record, ExtensionContext context, SessionWorldImpl world) {
        super(record, context);
        this.record = record;
        this.world = world;
    }

    @Override
    public boolean isEnabled() {
        return record.getIsEnabled();
    }

    @Override
    public int getTick() {
        return record.getTickNum();
    }

    @Override
    public String content() {
        return record.getContent();
    }

    @Contract(" -> new")
    public @NotNull SessionLocationImpl getLocation() {
        return world.locationOf(this);
    }

    @Contract(" -> new")
    @Override
    public @NotNull ChatCompletionMessage asChatCompletion() {
        return new ChatCompletionMessage(ChatCompletionRole.fromWireValue(record.getRole()),null, record.getContent());
    }

    @Override
    public Reference asReference() {
        return new ChatMessage.Reference(this.record.getSessionId(), this.record.getTickNum());
    }

    @Override
    public String toString() {
        return """
            ---
            tick %s:
            %s
            ---
            """.formatted(
                    this.record.getTickNum(),
            asChatCompletion().toString()
        );
    }
}
