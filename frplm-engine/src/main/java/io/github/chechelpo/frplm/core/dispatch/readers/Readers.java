package io.github.chechelpo.frplm.core.dispatch.readers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class Readers {

    private Readers() {
    }

    record Record(
            @NotNull EntityReader<LlmConnectionRecord> connections,
            @NotNull EntityReader<ApiHostsRecord> hosts,
            @NotNull EntityReader<ApiKeysRecord> secrets,

            @NotNull EntityReader<TagsRecord> tags,

            @NotNull EntityReader<CharactersRecord> characters,
            @NotNull EntityReader<CharacterTagsRecord> characterTags,
            @NotNull EntityReader<StartingLocationsRecord> startingLocations,

            @NotNull EntityReader<WorldsRecord> worlds,
            @NotNull EntityReader<RegionRecord> regions,
            @NotNull EntityReader<LocationsRecord> locations,
            @NotNull EntityReader<LocationEdgesRecord> edges,

            @NotNull EntityReader<LorebooksRecord> lorebooks,
            @NotNull EntityReader<EntryRecord> entries,
            @NotNull EntityReader<KeywordRecord> keywords,
            @NotNull EntityReader<EntryKeywordsRecord> entryKeywords,

            @NotNull EntityReader<OutletRecord> outlets,
            @NotNull EntityReader<PromptTemplateRecord> templates,
            @NotNull EntityReader<PromptSectionRecord> sections,

            @NotNull EntityReader<SessionsRecord> sessions,
            @NotNull EntityReader<MessagesRecord> messages
    ) {
        public List<EntityReader<?>> asList() {
            return List.of(
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
}