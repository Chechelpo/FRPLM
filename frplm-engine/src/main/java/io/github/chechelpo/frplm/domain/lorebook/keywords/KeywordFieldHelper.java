package io.github.chechelpo.frplm.domain.lorebook.keywords;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.KEYWORD;

@Component
final class KeywordFieldHelper extends ABSControllerAwareHelper<KeywordRecord, KeywordServiceImpl, KeywordController> {
    public KeywordFieldHelper(KeywordServiceImpl service, KeywordController controller) {
        super(service, controller);
        register_field(
                "id",
                KEYWORD.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .build()
        );

        register_field(
                "name",
                KEYWORD.KEYWORD_,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraint.builder()
                                        .readOnly()
                                        .build()
                        )
                        .requireOnCreate()
                        .build()
        );
    }
}
