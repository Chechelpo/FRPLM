package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;

public record CharacterJSON(
        String name,
        String description,
        Boolean can_be_user,

        Integer Ttl,
        Boolean is_static,
        String reason_why,

        String welcome_message,
        LorebookJSON lorebook
){}
