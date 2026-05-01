package chechelpo.demo.domain.character.tags;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.EntityKey;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.demo.jooq.generated.tables.records.TagsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.demo.jooq.generated.tables.CharacterTags.CHARACTER_TAGS;
import static chechelpo.demo.jooq.generated.tables.Tags.TAGS;

@Component
final class CharacterTagsStore extends ABSEntityStore<CharacterTagsRecord> {
    public CharacterTagsStore(@NotNull DSLContext ctx) {
        super(ctx, CHARACTER_TAGS, EntityTypes.Types.CHARACTER_TAGS);
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
