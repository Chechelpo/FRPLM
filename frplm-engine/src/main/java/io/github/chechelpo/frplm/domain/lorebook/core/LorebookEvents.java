package io.github.chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
final class LorebookEvents {

    private final LorebookService lorebookService;
    private final CharacterService characterService;
    private final EnumSet<EntityConfigs.Types> changeNamesConfigs = EnumSet.of(
            EntityConfigs.Types.CHARACTER, EntityConfigs.Types.LOCATIONS,
            EntityConfigs.Types.WORLDS, EntityConfigs.Types.REGIONS
    );

    LorebookEvents(LorebookService lorebookService, CharacterService characterService) {
        this.lorebookService = lorebookService;
        this.characterService = characterService;
    }

    private boolean updateLorebookName(int lorebookId, String newName){
        Objects.requireNonNull(newName);
        return lorebookService.update(
                EntityKey.of(LOREBOOKS.ID, lorebookId),
                EntityDataPayload.of(LOREBOOKS.NAME, newName)
        );
    }
    @SuppressWarnings("unchecked")
    @EventListener
    public void onLocationNameUpdateUpdateLorebook(CRUDCommittedEvent.UpdatedEntity<?> rawEvent){
        if (!changeNamesConfigs.contains(rawEvent.type())) return;

        switch (rawEvent.type()){
            case EntityConfigs.Types.WORLDS -> handleWorldNameChange(
                    (CRUDCommittedEvent.UpdatedEntity<WorldsRecord>) rawEvent
            );
            case EntityConfigs.Types.REGIONS -> handleRegionNameChange(
                    (CRUDCommittedEvent.UpdatedEntity<RegionRecord>) rawEvent
            );
            case EntityConfigs.Types.LOCATIONS -> handleLocationNameChange(
                    (CRUDCommittedEvent.UpdatedEntity<LocationsRecord>) rawEvent
            );
            case EntityConfigs.Types.CHARACTER -> handleCharacterNameChange(
                    (CRUDCommittedEvent.UpdatedEntity<CharactersRecord>) rawEvent
            );
        }
    }

    private void handleWorldNameChange(CRUDCommittedEvent.UpdatedEntity<WorldsRecord> worldEvent){}
    private void handleLocationNameChange(CRUDCommittedEvent.UpdatedEntity<LocationsRecord> locationEvent){}
    private void handleRegionNameChange(CRUDCommittedEvent.UpdatedEntity<RegionRecord> regionEvent){}
    private void handleCharacterNameChange(CRUDCommittedEvent.UpdatedEntity<CharactersRecord> characterEvent){}

}
