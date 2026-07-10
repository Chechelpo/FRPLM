package chechelpo.frplm.domain.sessions.messages;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Store
final class MessageStore extends EntityStore<MessagesRecord> {
    MessageStore(@NotNull DSLContext ctx) {
        super(ctx, MESSAGES, EntityTypes.Types.MESSAGES);
    }

    @NotNull List<MessagesRecord> getMessagesOf(int sessionID){
        return ctx.selectFrom(main_table)
                .where(MESSAGES.SESSION_ID.eq(sessionID))
                .orderBy(MESSAGES.TICK_NUM)
                .fetch();
    }

    MessagesRecord getLastMessage(int sessionID){
        return ctx.select()
                .from(MESSAGES)
                .join(SESSIONS)
                .on(
                        MESSAGES.SESSION_ID.eq(SESSIONS.ID).and(
                        MESSAGES.TICK_NUM.eq(SESSIONS.CURRENT_TICK))
                )
                .where(MESSAGES.SESSION_ID.eq(sessionID))
                .orderBy(MESSAGES.TICK_NUM.desc())
                .limit(1)
                .fetchOneInto(MessagesRecord.class);
    }

    List<MessagesRecord> getLast(int sessionId, int number){
        return ctx.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(sessionId))
                .orderBy(MESSAGES.TICK_NUM.desc())
                .limit(number)
                .fetch();
    }

    @NonNull List<MessagesRecord> getLast(int sessionId, int from, int to){
        return ctx.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(sessionId))
                .and(MESSAGES.TICK_NUM.between(from, to))
                .orderBy(MESSAGES.TICK_NUM.desc())
                .fetch();
    }
}
