package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.KEYWORD;

@Component
final class KeywordFieldHelper extends ABSControllerAwareHelper<KeywordRecord, KeywordService, KeywordController> {
    public KeywordFieldHelper(KeywordService service, KeywordController controller) {
        super(service, controller);
        register_field(
                "id",
                KEYWORD.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
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
                                StringConstraints.builder()
                                        .readOnly()
                                        .build()
                        )
                        .require()
                        .build()
        );
    }
}
