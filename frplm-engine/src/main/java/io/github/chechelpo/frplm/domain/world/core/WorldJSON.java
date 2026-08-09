package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.domain.world.edge.EdgeJSON;
import io.github.chechelpo.frplm.domain.world.location.LocationJSON;
import io.github.chechelpo.frplm.domain.world.region.RegionJSON;

import java.util.List;

public record WorldJSON(
        String name,
        String description,

        Double backgroundX,
        Double backgroundY,
        Double backgroundWidth,
        Double backgroundHeight,

        double backgroundOpacity,
        boolean backgroundVisible,
        boolean backgroundTransformLocked,
        boolean backgroundAspectLocked,
        String backgroundFit,

        LorebookJSON lorebook,
        List<RegionJSON> regions,
        List<LocationJSON> locations,
        List<EdgeJSON> edges
) {}
