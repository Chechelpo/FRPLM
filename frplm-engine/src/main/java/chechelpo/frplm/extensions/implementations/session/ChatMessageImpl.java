package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public final class ChatMessageImpl implements ChatMessage {
    private final MessagesRecord record;
    private final SessionWorldImpl world;

    public ChatMessageImpl(MessagesRecord record, SessionWorldImpl world) {
        this.record = record;
        this.world = world;
    }

    MessagesRecord getRecord() {
        return record;
    }

    @Override
    public int getMessageNumber() {
        return record.getTickNum();
    }
    public @NotNull ChatCompletionMessage asCompletionMessage() {
        ChatCompletionRole role = ChatCompletionRole.fromWireValue(record.getRole());
        return new ChatCompletionMessage(role, record.getContent());
    }
    @Contract(" -> new")
    public @NotNull SessionLocationImpl getLocation() {
        return world.locationOf(this);
    }

    @Contract(" -> new")
    @Override
    public @NotNull ChatCompletionMessage asChatCompletion() {
        return new ChatCompletionMessage(ChatCompletionRole.fromWireValue(record.getRole()), record.getContent());
    }
}
