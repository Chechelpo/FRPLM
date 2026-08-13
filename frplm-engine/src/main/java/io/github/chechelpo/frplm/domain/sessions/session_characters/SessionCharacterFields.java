package io.github.chechelpo.frplm.domain.sessions.session_characters;

import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSION_CHARACTERS;

@Component
public final class SessionCharacterFields extends EntityControllerFieldValidator<SessionCharactersRecord> {
    SessionCharacterFields() {
        super(EntityConfigs.Types.SESSION_CHARACTER, SESSION_CHARACTERS);
    }

    @Override
    protected List<FieldInfo<SessionCharactersRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(SESSION_CHARACTERS.SESSION_ID)
                        .requireOnCreate()
                        .build()
        );
    }

    @Override
    protected List<DTOField<SessionCharactersRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(SESSION_CHARACTERS.ID, "id"),
                DTOField.of(SESSION_CHARACTERS.SESSION_ID, "session_id"),

                DTOField.of(SESSION_CHARACTERS.NAME, "name"),
                DTOField.of(SESSION_CHARACTERS.DESCRIPTION, "description"),

                DTOField.of(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, "permanent_character_id"),
                DTOField.of(SESSION_CHARACTERS.KEEP_UPDATED, "keep_updated"),

                DTOField.of(SESSION_CHARACTERS.SESSION_LOREBOOK_ID, "session_lorebook_id"),
                DTOField.of(SESSION_CHARACTERS.WORLD_ID, "world_id"),
                DTOField.of(SESSION_CHARACTERS.CURRENT_LOCATION_ID, "current_location_id")
        );
    }
}
