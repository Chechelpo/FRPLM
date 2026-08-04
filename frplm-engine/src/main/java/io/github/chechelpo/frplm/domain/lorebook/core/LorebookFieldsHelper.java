package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.core.entities.fields.CommonFields;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
final class LorebookFieldsHelper extends EntityControllerFieldValidator<LorebooksRecord>{
    LorebookFieldsHelper() {
        super(EntityConfigs.Types.LOREBOOKS);
    }

    @Override
    protected List<DTOField<LorebooksRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(LOREBOOKS.ID, "id"),
                DTOField.of(LOREBOOKS.NAME, "name"),
                DTOField.of(LOREBOOKS.DEFAULT_OUTLET_ID, "default_outlet_id")
        );
    }

    @Override
    protected List<FieldInfo<LorebooksRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(LOREBOOKS.ID)
                        .key()
                        .build(),

                FieldInfo.builder(LOREBOOKS.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOREBOOKS.DEFAULT_OUTLET_ID)
                        .setDefaultValue(StandardOutlet.LOREBOOK.stable_id)
                        .build()
        );
    }
}
