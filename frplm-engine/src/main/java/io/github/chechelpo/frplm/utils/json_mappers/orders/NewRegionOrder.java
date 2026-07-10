package io.github.chechelpo.frplm.utils.json_mappers.orders;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;

public record NewRegionOrder(
        EntityDataPayload<RegionRecord> payload,
        NewLorebookOrder lorebookOrder,
        String parentRegionName
) {}
