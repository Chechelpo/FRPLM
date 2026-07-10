package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.jetbrains.annotations.TestOnly;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;
import static org.jooq.impl.DSL.max;

@Component
final class OutletHelper extends ABSControllerAwareHelper<OutletRecord, OutletService, OutletController> {
    private final DSLContext ctx;
    OutletHelper(OutletService service, OutletController controller, DSLContext dslContext) {
        super(service, controller);
        this.ctx = dslContext;
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

        Arrays.stream(StandardOutlet.values())
                .forEach(
                        outlet -> outlet.toKey().ifPresent( key -> {
                            if (!service.exists(key)) service.createAndGet(outlet.toPayload().orElseThrow());
                        })
                );
        restartIdentityAfterCurrentMax();
    }

    @TestOnly
    void ensureStandardOutlets(){
        Arrays.stream(StandardOutlet.values())
                .forEach(
                        outlet -> outlet.toKey().ifPresent( key -> {
                            if (!service.exists(key)) service.createAndGet(outlet.toPayload().orElseThrow());
                        })
                );
        restartIdentityAfterCurrentMax();
    }


    void restartIdentityAfterCurrentMax() {
        Integer nextId = ctx.select(max(OUTLET.ID).plus(1))
                .from(OUTLET)
                .fetchOneInto(Integer.class);

        if (nextId == null) {
            nextId = 1;
        }

        ctx.execute("ALTER TABLE OUTLET ALTER COLUMN ID RESTART WITH " + nextId);
    }
}

