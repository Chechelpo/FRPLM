package io.github.chechelpo.frplm.domain.tags;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.CHARACTER_TAGS;
import static chechelpo.frplm.jooq.generated.tables.Tags.TAGS;

@Component
final class TagStore extends EntityStore<TagsRecord> {
    public TagStore(@NotNull DSLContext ctx) {
        super(ctx, TAGS, EntityConfigs.Types.TAGS);
    }

    int countUsages(@NotNull EntityKey<TagsRecord> key){
        Integer number = ctx.selectCount()
                .from(CHARACTER_TAGS)
                .where(CHARACTER_TAGS.TAG_ID.eq(key.getValue(TAGS.ID)))
                .fetchOne(0, Integer.class);
        if (number == null){
            throw new IllegalStateException("No tag found with id " + key.getValue(TAGS.ID));
        }
        return number;
    }
}
