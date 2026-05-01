package chechelpo.demo.domain.lorebook.core;

import chechelpo.demo.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.demo.jooq.generated.tables.Lorebooks;
import chechelpo.demo.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.demo.frameworks.entities.fields.CommonFields;
import chechelpo.demo.frameworks.entities.fields.FieldInfo;
import chechelpo.demo.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.demo.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.demo.frameworks.entities.fields.format.StringFormat;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class LorebookFieldsHelper extends ABSFieldInstantiationHelper<
        LorebooksRecord,
        LorebookStore,
        LorebookService,
        LorebookController
        > {
    public LorebookFieldsHelper(
            LorebookStore store,
            LorebookService service,
            LorebookController controller
    ) {
        super(store, service, controller);
        register_field(
                "id",
                Lorebooks.LOREBOOKS.ID,
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
                CommonFields.NAME.getFieldName(),
                Lorebooks.LOREBOOKS.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .setFormat(
                                StringFormat.builder()
                                        .setInfo("Lorebook name, works as metadata")
                                        .setType(StringFormat.Types.SHORT_TEXT)
                                        .build()
                        )
                        .require()
                        .build()

        );
    }
}
