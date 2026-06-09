package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record PromptRenderContext(
            CharactersRecord userCharacter,
            CharactersRecord[] presentCharacters,
            PromptTemplateRecord template,
            LocationsRecord currentLocation,
            List<LocationsRecord> neighbours,
            LorebooksRecord[] lorebooks
    ) {
        @Override
        public @NotNull String toString() {
            return """
                PromptRenderContext {
                 \s
                  Character      \s
                   %s
                  Template       \s
                  %s
                  CurrentLocation\s
                  %s
                  Neighbours \s
                  %s
                  Lorebooks      \s
                  %s
                 \s
                }
               """.formatted(
                    safe(userCharacter),
                    safe(template),
                    safe(currentLocation),
                    safe(neighbours),
                    formatLorebooks(lorebooks)
            );
        }

        private static String formatLorebooks(LorebooksRecord[] lorebooks) {
            if (lorebooks == null) {
                return "    null";
            }

            if (lorebooks.length == 0) {
                return "    <empty>";
            }

            return Arrays.stream(lorebooks)
                    .map(lorebook -> "    " + safe(lorebook))
                    .collect(Collectors.joining("\n"));
        }

        private static String safe(Object value) {
            return value == null ? "null" : value.toString();
        }
    }