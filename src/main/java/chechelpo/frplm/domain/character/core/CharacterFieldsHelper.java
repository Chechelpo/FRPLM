package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.frplm.jooq.generated.tables.Characters;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.frameworks.entities.fields.CommonFields;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.format.StringFormat;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class CharacterFieldsHelper extends ABSFieldInstantiationHelper<
        CharactersRecord
        , CharacterStore
        ,CharacterService
        , CharacterController
        > {
    CharacterFieldsHelper(
            CharacterService service,
            CharacterStore store,
            CharacterController controller
    ) {
        super(store, service, controller);
        register_field(
                "id",
                Characters.CHARACTERS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .build()
        );
        register_field(
                CommonFields.NAME.getFieldName(),
                Characters.CHARACTERS.NAME,
                FieldInfo.stringField()
                        .setFormat(new StringFormat.StringFormatBuilder()
                                .setInfo("Character name")
                                .setType(StringFormat.Types.SHORT_TEXT)
                                .build()
                        )
                        .setConstraints(
                                new StringConstraints.Builder()
                                .setMaxLength(255).build()
                        ).build()
        );
        register_field(
                "lorebook_id",
                Characters.CHARACTERS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                                .build()
                        ).build()
        );
    }
}
