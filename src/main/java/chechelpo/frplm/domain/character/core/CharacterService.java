package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

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
        EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
        lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.CHARACTER_INFO.stable_id);
        lorebookData.set(LOREBOOKS.NAME, data.getValue(CHARACTERS.NAME));

        LorebooksRecord record = lorebookService.createAndGet(lorebookData);
        data.set(CHARACTERS.LOREBOOK_ID, record.getId());

        super.beforeCreate(data, operationID);
    }

    public @NotNull List<CharactersRecord> getStartingAt(int worldID){
        return store.getStartingAtWorld(worldID);
    }
}

