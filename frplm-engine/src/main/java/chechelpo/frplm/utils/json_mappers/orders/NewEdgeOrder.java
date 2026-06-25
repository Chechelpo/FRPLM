package chechelpo.frplm.utils.json_mappers.orders;

/**
 *
 * @param fromName
 * @param toName
 * @param description
 * @param travel_cost
 */
public record NewEdgeOrder(
        String fromName,
        String toName,
        String description,
        int travel_cost
) {}
