package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.PromptSectionEntityImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

final class SectionMapper extends ReferenceMapper<PromptSectionRecord, PromptSectionEntitySnapshot.Reference, PromptSectionEntitySnapshot> {
    SectionMapper(EntityReader<PromptSectionRecord> reader) {
        super(
                PromptSectionEntitySnapshot.class,
                PromptSectionEntitySnapshot.Reference::fromString,
                PromptSectionEntityImpl::new,
                reference -> EntityKey.<PromptSectionRecord>builder()
                        .set(PROMPT_SECTION.PROMPT_ID, (short) reference.promptId())
                        .set(PROMPT_SECTION.SECTION_ID, (short) reference.sectionId())
                        .build(),
                reader
        );
    }

    @Override
    PromptSectionEntitySnapshot.Reference getExampleReference() {
        return new PromptSectionEntitySnapshot.Reference(1, 1);
    }
}
