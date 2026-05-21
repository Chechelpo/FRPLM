package chechelpo.frplm.domain.world.core.microservices;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.web.bind.annotation.*;

@RequestMapping(EntityTypes.WORLDS_URL)
@RestController
final class WorldController extends EntityController<
        WorldsRecord,
        WorldService
        >
{
    WorldController(WorldService service) {
        super(EntityTypes.Types.WORLDS, service);
    }
}
