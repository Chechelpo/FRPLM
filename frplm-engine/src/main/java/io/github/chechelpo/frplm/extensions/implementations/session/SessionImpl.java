package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.extensions.api.utils.FindResult;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.session.SessionPrompt;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/// # Session
/// API interface of a session snapshot
///
/// Can be generated for any of the following:
/// - New/regeneration of a message registered in session.
/// - Creation of session.
public final class SessionImpl implements Session {
    private final SessionsRecord record;
    private final ExtensionContext context;
    private final SessionContext sessionContext;

    private final SessionWorldImpl world;
    private final SessionCharacterImpl userCharacter;

    public SessionImpl(
            SessionsRecord record,
            @NotNull ExtensionContext standaloneContext,
            SessionContext sessionContext
    ) {
        this.record = record;
        this.sessionContext = sessionContext;
        this.context = standaloneContext;
        this.world = new SessionWorldImpl(standaloneContext.worlds().getWorldOf(this.record), this, standaloneContext);
        this.userCharacter = new SessionCharacterImpl(
                standaloneContext.characters().getUserCharacter(this.record),
                context,
                this,
                this.world
        );
    }

    SessionContext context() {
        return this.sessionContext;
    }

    public SessionsRecord getRecord() {
        return record;
    }

    @Override
    public int getCurrentTick() {
        return record.getCurrentTick();
    }

    @Override
    public @NonNull String getName(){
        return record.getName();
    }

    @Contract(" -> new")
    @Override
    public @NotNull Reference asReference() {
        return new Session.Reference(record.getId());
    }

    @Override
    public FindResult<SessionPrompt, ?, ?> getPrompt() {
        return context.templates().getOf(this.record)
                .mapResult(template -> new PromptSessionImpl(template, context, this));
    }

    @Contract("_ -> new")
    public @NotNull ChatMessage getLastMessage(boolean filterEnabled) {
        if (!filterEnabled) return new ChatMessageImpl(
                sessionContext.messages().getLastMessageOf(this.record),
                context,
                world
        );

        return new ChatMessageImpl(
                sessionContext.messages().getLastEnabled(this.record.getId()),
                context,
                world
        );
    }

    @Override
    public @UnmodifiableView @NonNull List<ChatMessage> getChatHistory(final boolean filterEnabled) {
        return fromStream(sessionContext.messages().getMessages(this.record).stream()
                .filter(record -> !filterEnabled || record.getIsEnabled())
        );
    }

    @Override
    public SessionCharacterImpl getUserCharacter() {
        return userCharacter;
    }

    @Override
    public SessionWorldImpl getWorld() {
        return world;
    }

    @Contract(" -> new")
    public @NotNull ChatMessageImpl lastMessage() {
        return new ChatMessageImpl(sessionContext.messages().getLastMessageOf(this.getRecord()), context, world);
    }

    @Override
    public @NotNull @Unmodifiable List<ChatMessage> getLastMessages(int number, final boolean filterEnabled) {
        if (filterEnabled)
            return fromStream(sessionContext.messages().getLastEnabledMessages(record.getId(), number).stream());

        return fromStream(sessionContext.messages().getLastMessagesOf(record.getId(), number).stream());
    }

    @Override
    public @NotNull @Unmodifiable List<ChatMessage> getLastMessagesRange(int from, int to){
        return fromStream(sessionContext.messages().getRange(record.getId(), from, to).stream());
    }

    private @NonNull @Unmodifiable List<ChatMessage> fromStream(@NonNull Stream<MessagesRecord> stream){
        return stream.map(record -> (ChatMessage) new ChatMessageImpl(record, context, world)).toList();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
