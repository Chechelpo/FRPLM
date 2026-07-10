package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Store
final class TemplateStore extends EntityStore<PromptTemplateRecord> {
    TemplateStore(@NotNull DSLContext ctx) {
        super(ctx, PROMPT_TEMPLATE, EntityConfigs.Types.PROMPT_TEMPLATES);
    }

    boolean updateMaxTokens(int connectionId, int newMaxTokens) {
        return ctx.update(main_table)
                .set(PROMPT_TEMPLATE.MAX_TOKENS, newMaxTokens)
                .where(PROMPT_TEMPLATE.CONNECTION_ID.eq(connectionId)
                        .and(PROMPT_TEMPLATE.MAX_TOKENS.greaterThan(newMaxTokens))
                )
                .execute() == 1;
    }
}
