package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.domain.character.core.CharacterJSON;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;

import java.util.List;

public record LocationJSON(
        String name,
        String description,
        String parent_region_name,

        boolean locked,
        double x,
        double y,
        Double radius,

        LorebookJSON lorebook,
        List<CharacterJSON> charactersHere
) {}
