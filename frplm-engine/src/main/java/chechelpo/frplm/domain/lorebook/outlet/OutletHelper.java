package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static chechelpo.frplm.jooq.generated.Tables.OUTLET;

@Component
final class OutletHelper extends ABSControllerAwareHelper<OutletRecord, OutletService, OutletController> {
    OutletHelper(OutletService service, OutletController controller) {
        super(service, controller);
        register_field(
                "id",
                OUTLET.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "name",
                OUTLET.OUTLET_,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        ensureStandardOutlets();
    }

    void ensureStandardOutlets(){
        Arrays.stream(StandardOutlet.values())
                .forEach(
                        outlet -> outlet.toKey().ifPresent( key -> {
                            if (!service.exists(key)) service.createAndGet(outlet.toPayload().orElseThrow());
                        })
                );
    }
}

