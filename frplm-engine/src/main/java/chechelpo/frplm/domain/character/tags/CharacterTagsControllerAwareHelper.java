package chechelpo.frplm.domain.character.tags;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.CharacterTags;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
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
