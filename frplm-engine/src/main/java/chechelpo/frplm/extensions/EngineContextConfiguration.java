package chechelpo.frplm.extensions;

import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.connection.api_hosts.HostService;
import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.LLMService;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.domain.prompts.section.SectionService;
import chechelpo.frplm.domain.prompts.template.TemplateService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.MessageService;
import chechelpo.frplm.domain.sessions.movement.Movements;
import chechelpo.frplm.domain.world.core.WorldService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.extensions.implementations.session.SessionContext;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
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
            LocationsService locations,
            EdgeService edges,
            LorebookService lorebooks,
            EntryService entries,
            KeywordService keywords,
            EntryKeywordService entryKeywords,

            OutletService outlets,
            TemplateService templates,
            SectionService sections
    ) {
        return new ExtensionContext(
                llmService,
                hosts,
                secretService,

                characters,
                startingLocations,

                worlds,
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