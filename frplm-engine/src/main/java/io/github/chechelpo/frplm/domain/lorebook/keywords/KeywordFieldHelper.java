package io.github.chechelpo.frplm.domain.lorebook.keywords;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.KEYWORD;

@Component
final class KeywordFieldHelper extends EntityControllerFieldValidator<KeywordRecord> {
    KeywordFieldHelper() {
        super(EntityConfigs.Types.KEYWORDS);
    }

    @Override
    protected List<DTOField<KeywordRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(KEYWORD.ID, "id"),
                DTOField.of(KEYWORD.KEYWORD_, "name")
        );
    }

    @Override
    protected List<FieldInfo<KeywordRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(KEYWORD.ID)
                        .key()
                        .build(),

                FieldInfo.builder(KEYWORD.KEYWORD_)
                        .readOnly()
                        .requireOnCreate()
                        .build()
        );
    }
}
