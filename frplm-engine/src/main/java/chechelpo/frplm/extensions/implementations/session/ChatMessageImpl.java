package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.utils.DetectedOutlet;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.extensions.implementations.standalone.StandaloneEntity;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public final class ChatMessageImpl extends StandaloneEntity<MessagesRecord> implements ChatMessage {
    private final MessagesRecord record;
    private final SessionWorldImpl world;

    public ChatMessageImpl(MessagesRecord record, ExtensionContext context, SessionWorldImpl world) {
        super(record, context);
        this.record = record;
        this.world = world;
    }

    MessagesRecord getRecord() {
        return record;
    }

    @Override
    public String content() {
        return record.getContent();
    }

    @Override
    public List<DetectedOutlet> getDetectedOutlets() {
        return null;
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
