package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.session.Session;
import chechelpo.frplm.extensions.api.session.SessionPrompt;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.extensions.implementations.standalone.PromptImpl;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    SessionsRecord getRecord() {
        return record;
    }

    @Contract(" -> new")
    @Override
    public @NotNull Reference reference() {
        return new Session.Reference(record.getId());
    }

    @Override
    public Optional<SessionPrompt> getPrompt() {
        return context.templates().getOf(this.record)
                .map(template -> new PromptSessionImpl(template, context, this));
    }

    @Contract(" -> new")
    public @NotNull ChatMessage getLastMessage() {
        return new ChatMessageImpl(
                sessionContext.messages().getLastOf(this.record.getId()),
                world
        );
    }

    @Override
    public @UnmodifiableView List<ChatMessage> getChatHistory() {
        return sessionContext.messages().getMessages(this.record).stream()
                .map(record -> new ChatMessageImpl(record, world))
                .collect(Collectors.toUnmodifiableList());
    }

    public SessionCharacterImpl getUserCharacter() {
        return userCharacter;
    }

    public SessionWorldImpl getWorld() {
        return world;
    }

    @Contract(" -> new")
    public @NotNull ChatMessageImpl lastMessage() {
        return new ChatMessageImpl(sessionContext.messages().getLastOf(this.getRecord()), world);
    }

    @Override
    @Contract("_ -> new")
    public @NotNull List<ChatMessage> getLastMessages(int number) {
        return getLast(number, getChatHistory());
    }

    private static <T> @NotNull List<T> getLast(int number, @NotNull List<T> of) {
        if (number <= 0 || of.isEmpty()) {
            return List.of();
        }


        int from = Math.max(0, of.size() - number);
        return List.copyOf(of.subList(from, of.size()));
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
