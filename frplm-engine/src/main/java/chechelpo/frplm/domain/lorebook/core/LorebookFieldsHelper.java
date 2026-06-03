package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.frameworks.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.frameworks.entities.fields.CommonFields;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class LorebookFieldsHelper extends ABSControllerAwareHelper<
        LorebooksRecord,
        LorebookService,
        LorebookController
        > {
    public LorebookFieldsHelper(
            LorebookService service,
            LorebookController controller
    ) {
        super(service, controller);
        register_field(
                "id",
                Lorebooks.LOREBOOKS.ID,
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
                CommonFields.NAME.getFieldName(),
                Lorebooks.LOREBOOKS.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .require()
                        .build()

        );

        register_field(
                null,
                Lorebooks.LOREBOOKS.NEXT_ENTRY_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );

        register_field(
                "default_outlet_id",
                Lorebooks.LOREBOOKS.DEFAULT_OUTLET_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build(),
                StandardOutlet.LOREBOOK.stable_id
        );
    }
}
