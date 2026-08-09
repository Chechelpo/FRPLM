package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.domain.lorebook.entry.EntryJSON;

import java.util.List;

public record LorebookJSON (
        String name,
        String default_outlet_id,
        List<EntryJSON> entries
){}
