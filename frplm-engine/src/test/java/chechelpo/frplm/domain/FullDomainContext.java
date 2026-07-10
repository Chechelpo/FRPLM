package chechelpo.frplm.domain;

import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.lorebook.entry.core.EntryTestContext;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.world.core.WorldTestContext;
import chechelpo.frplm.domain.world.edge.EdgeTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.domain.world.region.RegionTestContext;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestComponent
@Import({EdgeTestContext.class, CharacterCoreTestContext.class, EntryTestContext.class, WorldTestContext.class, SessionTestContext.class, RegionTestContext.class, LocationTestContext.class})
public class FullDomainContext {
    @Autowired
    public EntryTestContext entries;
    @Autowired
    public CharacterCoreTestContext characters;
    @Autowired
    public WorldTestContext worlds;
    @Autowired
    public SessionTestContext sessions;
    @Autowired
    public RegionTestContext regions;
    @Autowired
    public LocationTestContext locations;
    @Autowired
    public StartingLocationTestContext startingLocations;
    @Autowired
    public EdgeTestContext edges;

    public void reload(){
        worlds.reload();
        entries.reload();
        sessions.reload();
    }
}
