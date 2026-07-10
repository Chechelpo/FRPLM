package io.github.chechelpo.frplm.domain.lorebook.keywords;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class KeywordStore extends EntityStore<KeywordRecord> {
    KeywordStore(@NotNull DSLContext ctx) {
        super(ctx, KEYWORD, EntityConfigs.Types.KEYWORDS);
    }


    public Integer getWith(String name){
        return ctx.select(KEYWORD.ID)
                .from(main_table)
                .where(KEYWORD.KEYWORD_.eq(name))
                .fetchOne(KEYWORD.ID);
    }
    public boolean existsWith(String name){
        return ctx.fetchExists(
                ctx.selectOne()
                        .from(KEYWORD)
                        .where(KEYWORD.KEYWORD_.eq(name))
        );
    }
    public Integer createWith(String name){
        return ctx.insertInto(KEYWORD)
                .set(KEYWORD.KEYWORD_, name)
                .onDuplicateKeyIgnore()
                .returning(KEYWORD.ID)
                .fetchOne(KEYWORD.ID);
    }

    public int getOrCreate(String keyword) {
        String normalized = Objects.requireNonNull(keyword).trim();

        ctx.insertInto(KEYWORD)
                .set(KEYWORD.KEYWORD_, normalized)
                .onDuplicateKeyIgnore()
                .execute();

        //noinspection DataFlowIssue
        return ctx.select(KEYWORD.ID)
                .from(KEYWORD)
                .where(KEYWORD.KEYWORD_.eq(normalized))
                .fetchSingle(KEYWORD.ID);
    }
}
