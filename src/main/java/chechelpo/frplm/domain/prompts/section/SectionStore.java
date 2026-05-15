package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

@Store
final class SectionStore extends ABSEntityStore<PromptSectionRecord> {
    SectionStore(@NotNull DSLContext ctx) {
        super(ctx, PROMPT_SECTION, EntityTypes.Types.SECTIONS);
    }
}
