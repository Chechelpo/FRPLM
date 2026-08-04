package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public class CharacterService extends EntityService<CharactersRecord, CharacterStore>  {
    private final LorebookService lorebookService;

    CharacterService(
            CharacterStore store,
            FieldValidator<CharactersRecord> validator,
            LorebookService lorebookService,
            EventBus eventBus
    ) {
        super(store, validator, eventBus);
        this.lorebookService = lorebookService;
    }

    public EntityKey<CharactersRecord> keyOf(int characterId){
        return EntityKey.of(CHARACTERS.ID, characterId);
    }

    @Override
    protected void beforeCreate(EntityDataPayload<CharactersRecord> data, long operationID) {
        if (!data.assigns(CHARACTERS.LOREBOOK_ID)){
            EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
            lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.CHARACTER_INFO.stable_id);
            lorebookData.set(LOREBOOKS.NAME, data.require(CHARACTERS.NAME));

            LorebooksRecord record = lorebookService.createAndGet(lorebookData);
            data.set(CHARACTERS.LOREBOOK_ID, record.getId());
        }

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<CharactersRecord> id, long operationID, CharactersRecord record) {
        lorebookService.delete(
                lorebookService.keyOf(lorebookService.getLorebookOf(record))
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }

    public @NotNull List<CharactersRecord> getStartingAt(int worldID){
        return store.getStartingAtWorld(worldID);
    }
    public @NotNull List<CharactersRecord> getStartingAt(int worldID, int locationId){
        return store.getStartingAtLocation(worldID,locationId);
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public CharactersRecord getUserCharacter(@NotNull SessionsRecord record) throws EntityNotFound {
        return this.find(EntityKey.of(CHARACTERS.ID, record.getUserPersonaId()))
                .orElseThrow( notFound -> {
                    log.error("Couldn't find session user character: {}", notFound.toDebugString());
                    return new EntityNotFound((RecordFindResult.NotFound<?>) notFound, Severity.SYSTEM);
                });
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull CharactersRecord[] getCharacters(@NotNull List<CurrentLocationsRecord> records){
        IntSet characterIDs = IntSetFactory.ofValues(records.stream().map(CurrentLocationsRecord::getCharacterId).toList());
        return store.getCharacters(characterIDs);
    }
}

