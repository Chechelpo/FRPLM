package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Store
final class MessageStore extends EntityStore<MessagesRecord> {
    MessageStore(@NotNull DSLContext ctx) {
        super(ctx, MESSAGES, EntityConfigs.Types.MESSAGES);
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
    MessagesRecord getLastEnabled(int sessionId){
        return ctx.selectFrom(MESSAGES)
                .where(
                        MESSAGES.SESSION_ID.eq(sessionId)
                        .and(MESSAGES.IS_ENABLED.isTrue())
                )
                .orderBy(MESSAGES.TICK_NUM.desc())
                .limit(1)
                .fetchOne();
    }

    List<MessagesRecord> getLast(int sessionId, int number){
        Result<MessagesRecord> desc = ctx.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(sessionId))
                .orderBy(MESSAGES.TICK_NUM.desc())
                .limit(number)
                .fetch();

        return desc.sortAsc(MESSAGES.TICK_NUM);
    }

    List<MessagesRecord> getLastEnabled(int sessionId, int number){
        Result<MessagesRecord> desc = ctx.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(sessionId)
                        .and(MESSAGES.IS_ENABLED.isTrue()))
                .orderBy(MESSAGES.TICK_NUM.desc())
                .limit(number)
                .fetch();

        return desc.sortAsc(MESSAGES.TICK_NUM);
    }

    @NonNull List<MessagesRecord> getRange(int sessionId, int from, int to){
        int span = Math.max(0, to - from + 1);

        return ctx.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(sessionId))
                .and(MESSAGES.TICK_NUM.between(from, to))
                .orderBy(MESSAGES.TICK_NUM.desc())
                .limit(span)
                .fetch();
    }
}
