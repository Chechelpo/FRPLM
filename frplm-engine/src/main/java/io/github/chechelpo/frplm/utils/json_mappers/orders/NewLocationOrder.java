package io.github.chechelpo.frplm.utils.json_mappers.orders;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;

import java.util.List;

public record NewLocationOrder (
    EntityDataPayload<LocationsRecord> payload,
    String parentRegionName,
    NewLorebookOrder lorebookOrder,
    List<NewCharacterOrder> charactersStartingHere
) {}
