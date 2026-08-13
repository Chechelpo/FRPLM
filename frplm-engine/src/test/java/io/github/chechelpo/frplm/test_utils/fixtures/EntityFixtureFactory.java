package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class EntityFixtureFactory {

    private final LorebookService lorebookService;
    private final WorldService worldService;
    private final LocationsService locationsService;
    private final RegionService regionService;
    private final CharacterService characterService;
    private final EdgeService edgeService;

    public EntityFixtureFactory(LorebookService lorebookService, WorldService worldService, LocationsService locationsService, RegionService regionService, CharacterService characterService, EdgeService edgeService) {
        this.lorebookService = lorebookService;
        this.worldService = worldService;
        this.locationsService = locationsService;
        this.regionService = regionService;
        this.characterService = characterService;
        this.edgeService = edgeService;
    }

    public LorebookFixtures lorebook(String seed){
        return new LorebookFixtures(
                lorebookService,
                this,
                seed
        );
    }
    public WorldFixtures worlds(String seed){
        return new WorldFixtures(
                worldService,
                this,
                seed
        );
    }
    public LocationFixtures locations(String seed){
        return new LocationFixtures(
                locationsService,
                this,
                seed
        );
    }
    public RegionFixtures regions(String seed){
        return new RegionFixtures(
                regionService,
                this,
                seed
        );
    }
    public CharacterFixtures characters(String seed){
        return new CharacterFixtures(
                characterService,
                this,
                seed
        );
    }

    public EdgesFixtures edges(String seed){
        return new EdgesFixtures(
                edgeService,
                this,
                seed
        );
    }
}
