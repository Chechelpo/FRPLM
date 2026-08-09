package io.github.chechelpo.frplm.domain.lorebook.entry;

import java.util.Set;

public record EntryJSON (
        String name,
        String content,
        String embed_text,
        Set<String> keywords,
        String outlet,

        Boolean enabled,

        Short probability,
        Integer delay,
        Integer cooldown,
        Integer stick_through,
        Short injection_order,
        Short strategy,

        Boolean prevent_further_recursion,
        Boolean non_recursable,
        Boolean delay_until_recursion,

        Short scan_depth,
        Short group_id
){}
