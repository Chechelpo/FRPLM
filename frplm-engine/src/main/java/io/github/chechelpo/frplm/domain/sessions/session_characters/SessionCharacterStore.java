package io.github.chechelpo.frplm.domain.sessions.session_characters;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSION_CHARACTERS;

@Store
final class SessionCharacterStore extends EntityStore<SessionCharactersRecord> {
    SessionCharacterStore(@NotNull DSLContext ctx) {
        super(ctx, SESSION_CHARACTERS, EntityConfigs.Types.SESSION_CHARACTER);
    }
}
