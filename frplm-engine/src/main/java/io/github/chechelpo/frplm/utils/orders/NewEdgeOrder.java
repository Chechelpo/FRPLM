package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import org.jooq.TableField;

import java.util.Optional;
import java.util.Set;

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
) implements CreationOrder<LocationEdgesRecord> {
    @Override
    public Optional<String> getZipPath() {
        return Optional.empty();
    }

}
