package chechelpo.frplm.domain.prompts.template.microservices;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Store
final class TemplateStore extends ABSEntityStore<PromptTemplateRecord> {
    TemplateStore(@NotNull DSLContext ctx) {
        super(ctx, PROMPT_TEMPLATE, EntityTypes.Types.PROMPT_TEMPLATES);
    }
}
