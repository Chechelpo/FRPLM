package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.assets.EntityAssetController;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.REGIONS_URL;

@RestController
@RequestMapping(REGIONS_URL)
final class RegionAssetController extends EntityAssetController<RegionRecord> {
    RegionAssetController(
            EntityAssetStore<RegionRecord, ?> assetStore,
            DTOMapper<RegionRecord> mapper,
            EntityReader<RegionRecord> reader
    ) {
        super(assetStore, mapper, reader);
    }
}
