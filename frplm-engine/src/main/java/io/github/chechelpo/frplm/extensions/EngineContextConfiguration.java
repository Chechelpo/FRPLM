package io.github.chechelpo.frplm.extensions;

import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
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
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.sessions.movement.Movements;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionContext;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.utils.tokenizers.TokenizerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineContextConfiguration {

    @Bean
    public ExtensionContext standaloneContext(
            LLMService llmService,
            HostService hosts,
            SecretService secretService,

            CharacterService characters,
            StartingLocationsService startingLocations,

            WorldService worlds,
            RegionService regions,
            LocationsService locations,
            EdgeService edges,

            LorebookService lorebooks,
            EntryService entries,
            KeywordService keywords,
            EntryKeywordService entryKeywords,

            OutletService outlets,
            TemplateService templates,
            SectionService sections,
            TokenizerService tokenizerService) {
        return new ExtensionContext(
                llmService,
                tokenizerService,
                hosts,
                secretService,

                characters,
                startingLocations,

                worlds,
                regions,
                locations,
                edges,

                lorebooks,
                entries,
                keywords,
                entryKeywords,

                outlets,

                templates,
                sections
        );
    }

    @Bean
    public SessionContext sessionContext(
            Movements movements,
            MessageService messages,
            SessionService session
    ) {
        return new SessionContext(
                movements,
                messages,
                session
        );
    }
}