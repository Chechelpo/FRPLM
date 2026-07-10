package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.Characters;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.core.entities.fields.CommonFields;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

@Component
final class CharacterFieldsHelper extends ABSControllerAwareHelper<
        CharactersRecord,
        CharacterService,
        CharacterController
        > {

    CharacterFieldsHelper(
            CharacterService service,
            CharacterController controller
    ) {
        super(service, controller);
        register_field(
                "id",
                Characters.CHARACTERS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
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
                        .setConstraints(
                                new StringConstraint.StringConstraintsBuilder()
                                .setMaxLength(255).build()
                        ).build()
        );

        register_field(
                "description",
                CHARACTERS.DESCRIPTION,
                FieldInfo.stringField()
                        .build()
        );

        register_field(
                "is_archetype",
                CHARACTERS.IS_ARCHETYPE,
                FieldInfo.booleanField()
                        .build()
        );
        register_field(
                "firstMessage",
                CHARACTERS.WELCOME_MESSAGE,
                FieldInfo.stringField()
                        .build()
        );
        register_field(
                "can_be_user",
                CHARACTERS.CAN_BE_USER,
                FieldInfo.booleanField()
                        .build()
        );

        register_field(
                "lorebook_id",
                Characters.CHARACTERS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .build()
                        ).build()
        );
    }
}
