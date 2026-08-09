package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;

public record RegionJSON(
        String name,
        String description,
        String parent_name,

        boolean locked,

        double x,
        double y,
        double width,
        double height,

        double backgroundOpacity,
        boolean backgroundVisible,
        boolean backgroundAspectLocked,
        String backgroundFit,

        boolean collapsed,

        LorebookJSON lorebook
) {}
