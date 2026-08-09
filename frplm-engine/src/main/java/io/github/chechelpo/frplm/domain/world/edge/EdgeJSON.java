package io.github.chechelpo.frplm.domain.world.edge;

public record EdgeJSON(
        String fromLocationRegion,
        String fromName,
        String toLocationRegion,
        String toName,
        String edge_description,
        Boolean is_traversable,
        Boolean show_destination_name,
        Boolean show_destination_description
) {}
