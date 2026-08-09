package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.PromptImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

final class PromptMapper extends ReferenceMapper<PromptTemplateRecord, PromptSnapshot.Reference, PromptSnapshot> {
    PromptMapper(EntityReader<PromptTemplateRecord> reader) {
        super(
                PromptSnapshot.class,
                PromptSnapshot.Reference::fromString,
                PromptImpl::new,
                reference -> EntityKey.of(PROMPT_TEMPLATE.ID, (short) reference.id()),
                reader
        );
    }

    @Override
    PromptSnapshot.Reference getExampleReference() {
        return new PromptSnapshot.Reference(1);
    }
}
