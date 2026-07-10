package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.core.entities.fields.CommonFields;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
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
