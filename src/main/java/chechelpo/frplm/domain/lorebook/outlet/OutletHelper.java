package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.springframework.stereotype.Component;

import java.util.Map;

import static chechelpo.frplm.jooq.generated.Tables.OUTLET;

@Component
final class OutletHelper extends ABSControllerAwareHelper<OutletRecord, OutletService, OutletController> {
    OutletHelper(OutletService service, OutletController controller) {
        super(service, controller);
        register_field(
                "id",
                OUTLET.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "name",
                OUTLET.OUTLET_,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        EntityKey<OutletRecord> outletKey;
        for (StandardOutlet outlet : StandardOutlet.values()) {
            outletKey = EntityKey.of(OUTLET.ID, outlet.stable_id);
            if (!service.exists(outletKey))
                service.createAndGet(EntityDataPayload.fromValues(Map.of(
                        OUTLET.ID, outlet.stable_id,
                        OUTLET.OUTLET_, outlet.name)
                ));
        }
    }
}
