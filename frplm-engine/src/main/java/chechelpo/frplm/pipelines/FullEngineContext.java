package chechelpo.frplm.pipelines;

import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.LLMService;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.domain.prompts.section.SectionService;
import chechelpo.frplm.domain.prompts.template.TemplateService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.core.MessageService;
import chechelpo.frplm.domain.sessions.movement.CurrentLocationService;
import chechelpo.frplm.domain.world.core.WorldService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import org.jetbrains.annotations.NotNull;

public record FullEngineContext(
        @NotNull CharacterService characters,

        @NotNull WorldService worlds,
        @NotNull LocationsService locations,
        @NotNull EdgeService neighbours,

        @NotNull LorebookService lorebooks,
        @NotNull EntryService entries,
        @NotNull KeywordService keywords,

        @NotNull CurrentLocationService currentLocations,
        @NotNull MessageService messages,

        @NotNull OutletService outlets,
        @NotNull TemplateService templates,
        @NotNull SectionService sections,

        @NotNull LLMService llm,
        @NotNull SecretService secrets,

        @NotNull SessionService sessions
        ){}
