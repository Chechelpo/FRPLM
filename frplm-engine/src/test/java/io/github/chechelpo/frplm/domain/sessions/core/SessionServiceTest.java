package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UneditableField;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    @Nested
    class UserPersonaInvariants {

        private static final int USER_ID = 1;
        private static final int WORLD_ID = 1;

        private SessionStore sessionStore;
        private SessionService sessionService;
        private CharacterService characterService;
        private EntityKey<CharactersRecord> characterKey;

        @BeforeEach
        void setUp() {
            sessionStore = mock(SessionStore.class);
            when(sessionStore.getMainTable()).thenReturn(SESSIONS);

            @SuppressWarnings("unchecked")
            FieldValidator<SessionsRecord> validator = new SessionFieldsHelper();

            characterService = mock(CharacterService.class);
            EventBus bus = mock(EventBus.class);

            sessionService = new SessionService(
                    sessionStore,
                    validator,
                    characterService,
                    bus
            );

            characterKey = EntityKey.<CharactersRecord>builder()
                    .set(CHARACTERS.ID, USER_ID)
                    .set(CHARACTERS.WORLD_ID, WORLD_ID)
                    .build();
        }

        @Test
        void creationRejectsCharacterThatCannotBeUser() {
            CharactersRecord character = new CharactersRecord();
            character.setWorldId(WORLD_ID);
            character.setCanBeUser(false);

            when(characterService.find(characterKey))
                    .thenReturn(
                            EntityReader.RecordFindResult.found(
                                    characterKey,
                                    character
                            )
                    );

            assertThrows(
                    InvalidValue.class,
                    () -> sessionService.createAndGet(sessionPayload())
            );
        }

        @Test
        void creationRejectsOnNotFoundCharacter() {
            when(characterService.find(any(EntityKey.class)))
                    .thenReturn(EntityReader.RecordFindResult.<CharactersRecord>notFound(
                            EntityKey.of()
                    ));

            assertThrows(
                    EntityNotFound.class,
                    () -> sessionService.createAndGet(
                            sessionPayload()
                    )
            );
        }

        @Test
        void creationRejectsCharacterWithoutStartingLocation() {
            CharactersRecord character = new CharactersRecord();
            character.setWorldId(WORLD_ID);
            character.setCanBeUser(true);
            character.setStartingLocationId(null);

            when(characterService.find(characterKey))
                    .thenReturn(
                            EntityReader.RecordFindResult.found(
                                    characterKey,
                                    character
                            )
                    );

            assertThrows(
                    InvalidValue.class,
                    () -> sessionService.createAndGet(sessionPayload())
            );
        }

        @Test
        void rejectsUpdateOnUserPersona() {
            when(sessionStore.exists(any(EntityKey.class)))
                    .thenReturn(true);
            EntityKey<SessionsRecord> sessionKey = EntityKey.of(SESSIONS.ID, 1);

            assertThrows(
                    UneditableField.class,
                    () -> sessionService.update(
                            sessionKey,
                            EntityDataPayload.of(SESSIONS.USER_PERSONA_ID, 100)
                    )
            );
        }

        @Contract(" -> new")
        private @NonNull EntityDataPayload<SessionsRecord> sessionPayload() {
            return EntityDataPayload.<SessionsRecord>builder()
                    .set(SESSIONS.NAME, "name")
                    .set(SESSIONS.USER_PERSONA_ID, USER_ID)
                    .set(SESSIONS.WORLD_ID, WORLD_ID)
                    .build();
        }
    }
}