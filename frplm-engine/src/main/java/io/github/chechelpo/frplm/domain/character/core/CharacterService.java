package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class CharacterService extends EntityService<CharactersRecord, CharacterStore> {
    private final LorebookService lorebookService;
    private final WorldService worldService;

    CharacterService(
            CharacterStore store,
            FieldValidator<CharactersRecord> validator,
            LorebookService lorebookService,
            EventBus eventBus,
            WorldService worldService
    ) {
        super(store, validator, eventBus);
        this.lorebookService = lorebookService;
        this.worldService = worldService;
    }

    @Override
    protected void beforeCreate(EntityDataPayload<CharactersRecord> data, long operationID) {
        data.ifUnassignedGet(
                CHARACTERS.LOREBOOK_ID,
                () -> lorebookService.createAndGet(
                        EntityDataPayload.<LorebooksRecord>builder()
                                .set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.CHARACTER_INFO.stable_id)
                                .set(LOREBOOKS.NAME, data.requireNonNull(CHARACTERS.NAME))
                                .build(),
                        LOREBOOKS.ID
                )
        );

        data.ifUnassignedGet(
                CHARACTERS.ID,
                () -> worldService.incrementAndGet(
                        WORLDS.NEXT_CHARACTER_ID,
                        EntityKey.of(WORLDS.ID, data.requireNonNull(CHARACTERS.WORLD_ID))
                ).orElseThrow()
        );

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void afterSuccessfulUpdate(CharactersRecord previousData, EntityKey<CharactersRecord> key, @NonNull EntityDataPayload<CharactersRecord> updated, long operationID) {
        updated.getAssignment(CHARACTERS.NAME)
                .ifAssignedNotNull(
                        newName -> lorebookService.update(
                                LOREBOOKS.NAME, newName,
                                EntityKey.of(LOREBOOKS.ID, previousData.getLorebookId())
                        )
                );

        super.afterSuccessfulUpdate(previousData, key, updated, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<CharactersRecord> id, long operationID, @NonNull CharactersRecord record) {
        lorebookService.delete(
                EntityKey.of(LOREBOOKS.ID, record.getLorebookId())
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }
}

