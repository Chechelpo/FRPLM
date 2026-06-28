package chechelpo.frplm.domain.world.region;

import chechelpo.frplm.domain.world.core.WorldTestContext;
import chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

@TestComponent
@Import(WorldTestContext.class)
public class RegionTestContext implements DBReload {
    @Autowired
    WorldTestContext worlds;
    @Autowired
    public RegionService service;
    @Autowired
    RegionFields fields;

    @Override
    public void reload() {
        worlds.reload();
    }
}
