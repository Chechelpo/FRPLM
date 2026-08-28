package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.entry_state.EntryStateService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.sessions.session_characters.SessionCharacterService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import org.jooq.Table;
import org.springframework.boot.test.context.TestComponent;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@TestComponent
public class EntityFixtureFactory {

    private final LorebookService lorebookService;
    private final WorldService worldService;
    private final LocationsService locationsService;
    private final RegionService regionService;
    private final CharacterService characterService;
    private final EdgeService edgeService;
    private final SessionService sessionService;
    private final SessionCharacterService sessionCharacterService;
    private final MessageService messageService;
    private final EntryService entryService;
    private final EntryStateService entryStateService;

    public EntityFixtureFactory(
            LorebookService lorebookService,
            WorldService worldService,
            LocationsService locationsService,
            RegionService regionService,
            CharacterService characterService,
            EdgeService edgeService,
            SessionService sessionService,
            SessionCharacterService sessionCharacterService,
            MessageService messageService,
            EntryService entryService,
            EntryStateService entryStateService
    ) {
        this.lorebookService = lorebookService;
        this.worldService = worldService;
        this.locationsService = locationsService;
        this.regionService = regionService;
        this.characterService = characterService;
        this.edgeService = edgeService;
        this.sessionService = sessionService;
        this.sessionCharacterService = sessionCharacterService;
        this.messageService = messageService;
        this.entryService = entryService;
        this.entryStateService = entryStateService;
    }

    public LorebookFixtures lorebook(String seed){
        return new LorebookFixtures(
                lorebookService,
                this,
                getCollisionFreeSeed(LOREBOOKS, seed)
        );
    }
    public WorldFixtures worlds(String seed){
        return new WorldFixtures(
                worldService,
                this,
                getCollisionFreeSeed(WORLDS, seed)
        );
    }
    public LocationFixtures locations(String seed){
        return new LocationFixtures(
                locationsService,
                this,
                getCollisionFreeSeed(LOCATIONS, seed)
        );
    }
    public RegionFixtures regions(String seed){
        return new RegionFixtures(
                regionService,
                this,
                getCollisionFreeSeed(REGION, seed)
        );
    }
    public CharacterFixtures characters(String seed){
        return new CharacterFixtures(
                characterService,
                this,
                getCollisionFreeSeed(CHARACTERS, seed)
        );
    }

    public SessionCharacterFixture sesCharacters(String seed){
        return new SessionCharacterFixture(
                sessionCharacterService,
                this,
                getCollisionFreeSeed(SESSION_CHARACTERS, seed)
        );
    }

    public SessionFixtures sessions(String seed){
        return new SessionFixtures(
                sessionService,
                this,
                getCollisionFreeSeed(SESSIONS, seed)
        );
    }

    public MessageFixtures messages(String seed){
        return new MessageFixtures(
                messageService,
                this,
                getCollisionFreeSeed(MESSAGES, seed)
        );
    }

    public EdgesFixtures edges(String seed){
        return new EdgesFixtures(
                edgeService,
                this,
                getCollisionFreeSeed(LOCATION_EDGES, seed)
        );
    }

    public EntryFixtures entries(String seed){
        return new EntryFixtures(
                entryService,
                this,
                getCollisionFreeSeed(ENTRY, seed)
        );
    }

    public EntryStateFixtures entryStates(String seed){
        return new EntryStateFixtures(
                entryStateService,
                this,
                getCollisionFreeSeed(ENTRY_STATE, seed)
        );
    }

    private String getCollisionFreeSeed(Table<?> forTable, String baseSeed){
        return forTable.getUnqualifiedName() + "-" + baseSeed;
    }
}
