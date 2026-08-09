package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.assets.EntityAssetController;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.WORLDS_URL;

@RestController
@RequestMapping(WORLDS_URL)
final class WorldAssetController extends EntityAssetController<WorldsRecord> {
    WorldAssetController(
            EntityAssetStore<WorldsRecord, ?> assetStore,
            DTOMapper<WorldsRecord> mapper,
            EntityReader<WorldsRecord> reader
    ) {
        super(assetStore, mapper, reader);
    }
}
