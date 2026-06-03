package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.session.Session;
import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
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

    public SessionImpl(SessionsRecord record, @NotNull ExtensionContext standaloneContext, SessionContext sessionContext) {
        this.record = record;
        this.sessionContext = sessionContext;
        this.context = standaloneContext;

        try{
            this.world = new SessionWorldImpl(standaloneContext.worlds().getWorldOf(this.record),  this, standaloneContext);
        } catch (EntityNotFound e) {
            throw new IllegalStateException("Could not find world of " + this.record, e);
        }

        try{
            this.userCharacter = new SessionCharacterImpl(
                    standaloneContext.characters().getUserCharacter(this.record),
                    context,
                    this,
                    this.world
                    );
        } catch (EntityNotFound e) {
            throw new IllegalStateException("Could not find user character of " + this.record, e);
        }
    }

    SessionContext context(){
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
    public Optional<PromptSnapshot> getPrompt() {
        try{
            return context.templates().getOf(this.record)
                    .map(template -> new PromptImpl(template, context));
        } catch (EntityNotFound ignored) {
            return Optional.empty();
        }
    }

    @Contract(" -> new")
    public @NotNull ChatMessage getLastMessage(){
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
    public @NotNull ChatMessageImpl lastMessage(){
        return new ChatMessageImpl(sessionContext.messages().getLastOf(this.getRecord()), world);
    }
    @Override
    @Contract("_ -> new")
    public @NotNull List<ChatMessage> getLastMessages(int number) {
        return
    }
    private static <T> @NotNull List<T> getLast(int number, @NotNull List<T> of){
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
