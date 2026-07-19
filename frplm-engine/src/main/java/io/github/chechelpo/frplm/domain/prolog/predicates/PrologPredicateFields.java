package io.github.chechelpo.frplm.domain.prolog.predicates;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateRecord;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;

@Component
final class PrologPredicateFields extends ABSHelper<PrologPredicateRecord, PrologPredicateService> {
    PrologPredicateFields(PrologPredicateService service) {
        super(service);

        register_field(PROLOG_PREDICATE.ID)
                .setInfo(FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
                );

        register_field(PROLOG_PREDICATE.NAME)
                .setInfo(FieldInfo.stringField().build());

        register_field(PROLOG_PREDICATE.ARITY)
                .setInfo(FieldInfo.numberField(FieldType.SHORT)
                        .build()
                );

        register_field(PROLOG_PREDICATE.KIND)
                .setInfo(FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setPossibleValues(Arrays.stream(PrologPredicateKind.values())
                                        .map(PrologPredicateKind::getTableName)
                                        .toArray(String[]::new))
                        )
                        .build()
                );

        register_field(PROLOG_PREDICATE.DESCRIPTION)
                .setInfo(FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder().nullable())
                        .build()
                );

        register_field(PROLOG_PREDICATE.PROVIDER_KEY)
                .setInfo(FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .nullable()
                                .build()
                        )
                        .build()
                );

        register_field(PROLOG_PREDICATE.SOURCE)
                .setInfo(FieldInfo.stringField()
                        .build()
                );
    }
}
