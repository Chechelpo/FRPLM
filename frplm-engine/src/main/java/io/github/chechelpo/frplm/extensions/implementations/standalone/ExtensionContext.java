package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.connection.api_hosts.HostService;
import io.github.chechelpo.frplm.domain.connection.api_keys.SecretService;
import io.github.chechelpo.frplm.domain.connection.llm.LLMService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.domain.prompts.section.SectionService;
import io.github.chechelpo.frplm.domain.prompts.template.TemplateService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizerService;
import org.jetbrains.annotations.NotNull;

public record ExtensionContext(
        @NotNull LLMService connections,
        @NotNull TokenizerService tokenizers,
        @NotNull HostService hosts,
        @NotNull SecretService secrets,


        @NotNull CharacterService characters,

        @NotNull WorldService worlds,
        @NotNull RegionService regions,
        @NotNull LocationsService locations,
        @NotNull EdgeService edges,

        @NotNull LorebookService lorebooks,
        @NotNull EntryService entries,
        @NotNull KeywordService keywords,
        @NotNull EntryKeywordService entryKeywords,

        @NotNull OutletService outlets,
        @NotNull TemplateService templates,
        @NotNull SectionService sections
) {}
