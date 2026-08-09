package io.github.chechelpo.frplm.domain.character.tags;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.tables.CharacterTags.CHARACTER_TAGS;

@Component
public final class CharacterTagsControllerAwareHelper extends EntityControllerFieldValidator<CharacterTagsRecord> {
    CharacterTagsControllerAwareHelper() {
        super(EntityConfigs.Types.CHARACTER_TAGS);
    }

    @Override
    protected List<FieldInfo<CharacterTagsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(CHARACTER_TAGS.CHAR_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(CHARACTER_TAGS.TAG_ID)
                        .key()
                        .requireOnCreate()
                        .build()
        );
    }

    @Override
    protected List<DTOField<CharacterTagsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(CHARACTER_TAGS.CHAR_ID, "char_id"),
                DTOField.of(CHARACTER_TAGS.TAG_ID, "tag_id")
        );
    }
}
