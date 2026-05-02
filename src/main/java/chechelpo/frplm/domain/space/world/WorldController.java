package chechelpo.frplm.domain.space.world;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.web.bind.annotation.*;

@RequestMapping(EntityTypes.WORLDS_URL)
@RestController
final class WorldController extends ABSEntityController<
        WorldsRecord,
        WorldService
        >
{
    WorldController(WorldService service) {
        super(EntityTypes.Types.WORLDS, service);
    }
}
