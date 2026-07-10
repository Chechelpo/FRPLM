package io.github.chechelpo.frplm.domain.character.tags;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.CharacterTags;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import org.springframework.stereotype.Component;

@Component
final class CharacterTagsControllerAwareHelper
        extends ABSControllerAwareHelper<CharacterTagsRecord, CharacterTagsService, CharacterTagsController>
{
    public CharacterTagsControllerAwareHelper(CharacterTagsService service, CharacterTagsController controller) {
        super(service, controller);
        register_field(
                "char_id",
                CharacterTags.CHARACTER_TAGS.CHAR_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .require()
                        .build()
        );
        register_field(
                "tag_id",
                CharacterTags.CHARACTER_TAGS.TAG_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .require()
                        .build()
        );
    }
}
