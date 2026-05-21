package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Store
final class SectionStore extends EntityStore<PromptSectionRecord> {
    SectionStore(@NotNull DSLContext ctx) {
        super(ctx, PROMPT_SECTION, EntityTypes.Types.SECTIONS);
    }

    @NotNull List<PromptSectionRecord> getOrderedSections(@NotNull EntityKey<PromptTemplateRecord> templateKey) {
        return ctx.selectFrom(PROMPT_SECTION)
                .where(PROMPT_SECTION.PROMPT_ID.eq(templateKey.getValue(PROMPT_TEMPLATE.ID)))
                .orderBy(PROMPT_SECTION.POSITION)
                .fetch();
    }

}
