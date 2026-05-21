package chechelpo.frplm.domain.world.core.utils;

import chechelpo.frplm.domain.world.core.microservices.WorldService;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.stereotype.Component;

@Component
final class WorldRepository extends EntityRepository<WorldsRecord, WorldService> {
    WorldRepository(WorldService service) {
        super(service);
    }
}
