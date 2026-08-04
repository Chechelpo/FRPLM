package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;
import static org.jooq.impl.DSL.max;

@Component
final class OutletHelper extends EntityControllerFieldValidator<OutletRecord> {
    OutletHelper() {
        super(EntityConfigs.Types.OUTLET);
    }

    @Override
    protected List<DTOField<OutletRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(OUTLET.ID, "id"),
                DTOField.of(OUTLET.OUTLET_, "name")
        );
    }

    @Override
    protected List<FieldInfo<OutletRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(OUTLET.OUTLET_)
                        .readOnly()
                        .build(),

                FieldInfo.builder(OUTLET.ID)
                        .key()
                        .build()
        );
    }


    void restartIdentityAfterCurrentMax() {

    }
}

