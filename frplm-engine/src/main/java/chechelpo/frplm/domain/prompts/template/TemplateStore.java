package chechelpo.frplm.domain.prompts.template;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Store
final class TemplateStore extends EntityStore<PromptTemplateRecord> {
    TemplateStore(@NotNull DSLContext ctx) {
        super(ctx, PROMPT_TEMPLATE, EntityTypes.Types.PROMPT_TEMPLATES);
    }
}
