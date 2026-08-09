package io.github.chechelpo.frplm.core.dispatch.readers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReadersConfiguration {

    @Bean
    public Readers.Record readers(
            EntityReader<LlmConnectionRecord> connections,
            EntityReader<ApiHostsRecord> hosts,
            EntityReader<ApiKeysRecord> secrets,

            EntityReader<TagsRecord> tags,

            EntityReader<CharactersRecord> characters,
            EntityReader<CharacterTagsRecord> characterTags,
            EntityReader<StartingLocationsRecord> startingLocations,

            EntityReader<WorldsRecord> worlds,
            EntityReader<RegionRecord> regions,
            EntityReader<LocationsRecord> locations,
            EntityReader<LocationEdgesRecord> edges,

            EntityReader<LorebooksRecord> lorebooks,
            EntityReader<EntryRecord> entries,
            EntityReader<KeywordRecord> keywords,
            EntityReader<EntryKeywordsRecord> entryKeywords,

            EntityReader<OutletRecord> outlets,
            EntityReader<PromptTemplateRecord> templates,
            EntityReader<PromptSectionRecord> sections,

            @NotNull EntityReader<SessionsRecord> sessions,
            @NotNull EntityReader<MessagesRecord> messages
    ) {
        return new Readers.Record(
                connections,
                hosts,
                secrets,

                tags,

                characters,
                characterTags,
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
                sections,

                sessions,
                messages
        );
    }
}