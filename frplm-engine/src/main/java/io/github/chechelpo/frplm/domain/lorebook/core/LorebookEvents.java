package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityUpdater;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class LorebookEvents {

    private final LorebookService lorebookService;
    private final CharacterService characterService;
    private final EnumSet<EntityConfigs.Types> changeNamesConfigs = EnumSet.of(
            EntityConfigs.Types.CHARACTER, EntityConfigs.Types.LOCATIONS,
            EntityConfigs.Types.WORLDS, EntityConfigs.Types.REGIONS
    );
    private final WorldService worldService;
    private final RegionService regionService;
    private final LocationsService locationsService;

    LorebookEvents(LorebookService lorebookService, CharacterService characterService, WorldService worldService, RegionService regionService, LocationsService locationsService) {
        this.lorebookService = lorebookService;
        this.characterService = characterService;
        this.worldService = worldService;
        this.regionService = regionService;
        this.locationsService = locationsService;
    }

    private EntityUpdater.UpdateResult<LorebooksRecord> updateLorebookName(int lorebookId, String newName){
        Objects.requireNonNull(newName);
        return lorebookService.update(
                EntityKey.of(LOREBOOKS.ID, lorebookId),
                EntityDataPayload.of(LOREBOOKS.NAME, newName)
        );
    }
    @SuppressWarnings("unchecked")
    @EventListener
    public void onLocationNameUpdateUpdateLorebook(CRUDCommittedEvent.UpdatedEntity<?> rawEvent){
        Objects.requireNonNull(rawEvent, "Event is null");
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

    private static @NonNull String getMessage(String name) {
        return "Couldn't update " + name + "'s lorebook";
    }
    private void handleWorldNameChange(CRUDCommittedEvent.UpdatedEntity<WorldsRecord> worldEvent) {
        if (!worldEvent.updatedData().assignsField(WORLDS.NAME)) return;
        WorldsRecord world = worldService.find(worldEvent.target())
                .orElseThrow("Couldn't find parent world when updating its lorebook name", Severity.SYSTEM);

        updateLorebookName(world.getLorebookId(), world.getName())
                .orElseThrow(getMessage(world.getName()), Severity.SYSTEM);
    }

    private void handleLocationNameChange(
            CRUDCommittedEvent.UpdatedEntity<LocationsRecord> locationEvent
    ) {
        if (!locationEvent.updatedData().assignsField(LOCATIONS.NAME)) return;
        String name = locationEvent.updatedData().requireValue(LOCATIONS.NAME);
        LocationsRecord location = locationsService.find(locationEvent.target())
                .orElseThrow("Couldn't find parent location (with name %s) when updating lorebook name".formatted(name), Severity.SYSTEM);

        updateLorebookName(location.getLorebookId(), location.getName())
                .orElseThrow(getMessage(location.getName()), Severity.SYSTEM);
    }

    private void handleRegionNameChange(
            CRUDCommittedEvent.UpdatedEntity<RegionRecord> regionEvent
    ) {
        if (!regionEvent.updatedData().assignsField(REGION.NAME)) return;
        String name = regionEvent.updatedData().requireValue(REGION.NAME);

        RegionRecord region = regionService.find(regionEvent.target())
                .orElseThrow(
                        "Couldn't find parent region (with new name %s) when updating lorebook with name ".formatted(name),
                        Severity.SYSTEM
                );

        updateLorebookName(region.getLorebookId(), region.getName())
                .orElseThrow(getMessage(region.getName()), Severity.SYSTEM);
    }

    private void handleCharacterNameChange(
            CRUDCommittedEvent.UpdatedEntity<CharactersRecord> characterEvent
    ) {
        if (!characterEvent.updatedData().assignsField(CHARACTERS.NAME)) return;

        CharactersRecord character = characterService.find(characterEvent.target())
                .orElseThrow(
                        "Couldn't find parent character when updating lorebook name",
                        Severity.SYSTEM
                );

       updateLorebookName(character.getLorebookId(), character.getName())
               .orElseThrow(getMessage(character.getName()), Severity.SYSTEM);
    }
}
