package io.github.chechelpo.frplm.domain.character.tags;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.tables.CharacterTags.CHARACTER_TAGS;
import static io.github.chechelpo.frplm.jooq.generated.tables.Tags.TAGS;

@Component
final class CharacterTagsStore extends EntityStore<CharacterTagsRecord> {
    public CharacterTagsStore(@NotNull DSLContext ctx) {
        super(ctx, CHARACTER_TAGS, EntityConfigs.Types.CHARACTER_TAGS);
    }

    @NotNull List<TagsRecord> getCharacterTags(@NotNull EntityKey<CharacterTagsRecord> id) {
        return ctx.select()
                .from(TAGS)
                .join(CHARACTER_TAGS)
                .on(TAGS.ID.eq(CHARACTER_TAGS.TAG_ID))
                .where(id.getEqualityConditions())
                .fetchInto(TagsRecord.class);
    }
}
