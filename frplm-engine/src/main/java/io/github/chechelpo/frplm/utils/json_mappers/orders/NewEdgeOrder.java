package io.github.chechelpo.frplm.utils.json_mappers.orders;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;

/**
 * An order describing the creation of an edge between two locations.
 * @param fromRegion parent region of the from location
 * @param fromName from location
 * @param toRegion parent region of the to location
 * @param toName to location with name
 * @param payload edge information
 */
public record NewEdgeOrder(
        String fromRegion,
        String fromName,
        String toRegion,
        String toName,
        EntityDataPayload<LocationEdgesRecord> payload
) {}
