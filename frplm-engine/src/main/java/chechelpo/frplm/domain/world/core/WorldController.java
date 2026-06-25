package chechelpo.frplm.domain.world.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@RequestMapping(EntityTypes.WORLDS_URL)
@Component
@RestController
public final class WorldController extends EntityController<
        WorldsRecord,
        WorldService
        >
{
    WorldController(WorldService service) {
        super(service);
    }
}
