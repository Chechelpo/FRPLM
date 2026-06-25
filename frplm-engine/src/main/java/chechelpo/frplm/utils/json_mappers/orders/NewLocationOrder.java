package chechelpo.frplm.utils.json_mappers.orders;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;

import java.util.List;

public record NewLocationOrder (
    EntityDataPayload<LocationsRecord> payload,
    NewLorebookOrder lorebookOrder,
    List<NewCharacterOrder> charactersStartingHere
) {}
