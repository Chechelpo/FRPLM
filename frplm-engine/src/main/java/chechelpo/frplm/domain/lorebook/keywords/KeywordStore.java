package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.Keyword;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class KeywordStore extends EntityStore<KeywordRecord> {
    KeywordStore(@NotNull DSLContext ctx) {
        super(ctx, KEYWORD, EntityTypes.Types.KEYWORDS);
    }

    public @NotNull IntObjectPair<String>[] getKeywordsOf(IntSet lorebookIDs) {
        return ctx.selectDistinct(KEYWORD.KEYWORD_, KEYWORD.ID)
                .from(KEYWORD)
                .join(ENTRY_KEYWORDS)
                .on(KEYWORD.ID.eq(ENTRY_KEYWORDS.KEYWORD_ID))
                .join(ENTRY)
                .on(
                        ENTRY_KEYWORDS.LOREBOOK_ID.eq(ENTRY.LOREBOOK_ID)
                                .and(ENTRY_KEYWORDS.ENTRY_ID.eq(ENTRY.ENTRY_ID))
                )
                .where(ENTRY_KEYWORDS.LOREBOOK_ID.in(lorebookIDs))
                .fetch(record -> IntObjectPair.of(record.get(KEYWORD.ID), record.get(KEYWORD.KEYWORD_)))
                .toArray(IntObjectPair[]::new);
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
                .returning(KEYWORD.ID)
                .fetchOne(KEYWORD.ID);
    }
}
