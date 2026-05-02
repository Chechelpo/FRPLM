package chechelpo.frplm.domain.character.tags;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.frplm.jooq.generated.tables.CharacterTags;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import org.springframework.stereotype.Component;

@Component
final class CharacterTagsFieldInstantiationHelper
        extends ABSFieldInstantiationHelper<CharacterTagsRecord, CharacterTagsStore, CharacterTagsService, CharacterTagsController>
{
    public CharacterTagsFieldInstantiationHelper(CharacterTagsStore store, CharacterTagsService service, CharacterTagsController controller) {
        super(store, service, controller);
        register_field(
                "char_id",
                CharacterTags.CHARACTER_TAGS.CHAR_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .require()
        );
        register_field(
                "tag_id",
                CharacterTags.CHARACTER_TAGS.TAG_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .require()
        );
    }
}
