package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.WORLDS_URL;

@RequestMapping(WORLDS_URL)
@Component
@RestController
public final class WorldController extends EntityController<
        WorldsRecord,
        WorldService
        >
{
    WorldController(WorldService service, DTOMapper<WorldsRecord> mapper) {
        super(EntityConfigs.Types.WORLDS, service, mapper);
    }
}
