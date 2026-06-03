package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;

import java.util.Arrays;
import java.util.stream.Collectors;

public record PromptRenderContext(
            CharactersRecord character,
            PromptTemplateRecord template,
            LocationsRecord currentLocation,
            LorebooksRecord[] lorebooks
    ) {
        @Override
        public String toString() {
            return """
                PromptRenderContext {
                  character      \s
                   %s
                  template       \s
                  %s
                  currentLocation\s
                  %s
                  lorebooks      \s
                  %s
                 \s
                }
               \s""".formatted(
                    safe(character),
                    safe(template),
                    safe(currentLocation),
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