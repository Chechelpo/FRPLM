package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.Characters;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

@Component
final class CharacterFieldsHelper extends EntityControllerFieldValidator<CharactersRecord> {
    CharacterFieldsHelper() {
        super(EntityConfigs.Types.CHARACTER);
    }

    @Contract(" -> new")
    @Override
    protected @NonNull @Unmodifiable List<FieldInfo<CharactersRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(CHARACTERS.ID)
                        .key()
                        .build(),

                FieldInfo.builder(Characters.CHARACTERS.LOREBOOK_ID)
                        .readOnly()
                        .build()
        );
    }

    @Contract(" -> new")
    @Override
    protected @NonNull @Unmodifiable List<DTOField<CharactersRecord,?>> getDTOStructure() {
        return List.of(
                DTOField.of(CHARACTERS.ID, "id"),
                DTOField.of(CHARACTERS.NAME, "name"),
                DTOField.of(CHARACTERS.DESCRIPTION, "description"),
                DTOField.of(CHARACTERS.IS_ARCHETYPE, "is_archetype"),
                DTOField.of(CHARACTERS.WELCOME_MESSAGE, "welcome_message"),
                DTOField.of(CHARACTERS.CAN_BE_USER, "can_be_user"),
                DTOField.of(CHARACTERS.LOREBOOK_ID, "lorebook_id")
        );
    }
}
