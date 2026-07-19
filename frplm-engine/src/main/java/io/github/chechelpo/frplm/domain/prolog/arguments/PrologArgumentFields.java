package io.github.chechelpo.frplm.domain.prolog.arguments;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateArgumentRecord;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE_ARGUMENT;

@Component
final class PrologArgumentFields extends ABSHelper<PrologPredicateArgumentRecord, PrologArgumentService> {
    PrologArgumentFields(PrologArgumentService service) {
        super(service);

        register_field(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID)
                .setInfo(FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .requireOnCreate()
                        .build()
                );

        register_field(PROLOG_PREDICATE_ARGUMENT.POSITION)
                .setInfo(FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .requireOnCreate()
                        .build()
                );

        register_field(PROLOG_PREDICATE_ARGUMENT.NAME).setInfo(FieldInfo.stringField().build());

        register_field(PROLOG_PREDICATE_ARGUMENT.TYPE)
                .setInfo(FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setPossibleValues(
                                        Arrays.stream(PrologArgumentType.values())
                                                .map(PrologArgumentType::getTableValue)
                                                .toArray(String[]::new)
                                )
                        )
                        .requireOnCreate()
                        .build()
                )
                .withDefaultValue(PrologArgumentType.TEXT.getTableValue());
    }
}
