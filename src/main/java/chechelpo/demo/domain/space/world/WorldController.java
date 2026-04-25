package chechelpo.demo.domain.space.world;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.WorldsRecord;
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
