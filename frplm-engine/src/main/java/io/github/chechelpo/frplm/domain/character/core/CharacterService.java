package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public class CharacterService extends EntityService<CharactersRecord, CharacterStore>  {
    private final LorebookService lorebookService;
    CharacterService(CharacterStore store, LorebookService lorebookService, EventBus eventBus) {
        super(store, eventBus);
        this.lorebookService = lorebookService;
    }

    @Override
    protected void beforeCreate(EntityDataPayload<CharactersRecord> data, long operationID) {
        if (!data.assignsField(CHARACTERS.LOREBOOK_ID)){
            EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
            lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.CHARACTER_INFO.stable_id);
            lorebookData.set(LOREBOOKS.NAME, data.requireValue(CHARACTERS.NAME));

            LorebooksRecord record = lorebookService.createAndGet(lorebookData);
            data.set(CHARACTERS.LOREBOOK_ID, record.getId());
        }

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<CharactersRecord> target, EntityDataPayload<CharactersRecord> data, long operationID) {
        super.beforeUpdate(target, data, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<CharactersRecord> id, long operationID, CharactersRecord record) {
        lorebookService.delete(
                lorebookService.keyOf(lorebookService.getLorebookOf(record))
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }

    public Optional<CharactersRecord> getCharacterWith(String name){
        return Optional.ofNullable(store.getWithName(name));
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
                .orElseThrow(() -> {
                    log.error("Session has no user character \n {}", record);
                    return new EntityNotFound("Session has no user character", Severity.SYSTEM);
                });
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull CharactersRecord[] getCharacters(@NotNull List<CurrentLocationsRecord> records){
        IntSet characterIDs = IntSetFactory.ofValues(records.stream().map(CurrentLocationsRecord::getCharacterId).toList());
        return store.getCharacters(characterIDs);
    }
}

