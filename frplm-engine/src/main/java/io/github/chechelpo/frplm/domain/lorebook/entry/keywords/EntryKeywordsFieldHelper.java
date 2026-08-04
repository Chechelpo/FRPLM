package io.github.chechelpo.frplm.domain.lorebook.entry.keywords;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY_KEYWORDS;

@Component
final class EntryKeywordsFieldHelper extends EntityControllerFieldValidator<EntryKeywordsRecord> {
    EntryKeywordsFieldHelper() {
        super(EntityConfigs.Types.ENTRY_KEYWORDS);
    }

    @Override
    protected List<DTOField<EntryKeywordsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(ENTRY_KEYWORDS.LOREBOOK_ID, "lorebook_id"),
                DTOField.of(ENTRY_KEYWORDS.ENTRY_ID,"entry_id"),
                DTOField.of(ENTRY_KEYWORDS.KEYWORD_ID, "keyword_id")
        );
    }

    @Override
    protected List<FieldInfo<EntryKeywordsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(ENTRY_KEYWORDS.LOREBOOK_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(ENTRY_KEYWORDS.ENTRY_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(ENTRY_KEYWORDS.KEYWORD_ID)
                        .key()
                        .requireOnCreate()
                        .build()
        );
    }
}
