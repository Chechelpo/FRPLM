package io.github.chechelpo.frplm.utils.json_mappers.orders;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;

/**
 * An order describing the creation of an edge between two locations.
 * @param fromName from location
 * @param toName to location with name
 * @param payload edge information
 */
public record NewEdgeOrder(
        String fromName,
        String toName,
        EntityDataPayload<LocationEdgesRecord> payload
) {}
