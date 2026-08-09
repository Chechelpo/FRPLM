package io.github.chechelpo.frplm.domain.prolog.arguments;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateArgumentRecord;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE_ARGUMENT;

@Component
final class PrologArgumentFields
        extends EntityFieldsValidator<PrologPredicateArgumentRecord> {

    PrologArgumentFields() {
        super();
    }

    @Override
    protected List<FieldInfo<PrologPredicateArgumentRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(PROLOG_PREDICATE_ARGUMENT.PREDICATE_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE_ARGUMENT.POSITION)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE_ARGUMENT.NAME)
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE_ARGUMENT.TYPE)
                        .addAllowedValues(
                                Arrays.stream(PrologArgumentType.values())
                                        .map(PrologArgumentType::getTableValue)
                                        .toList()
                        )
                        .setDefaultValue(PrologArgumentType.TEXT.getTableValue())
                        .build()
        );
    }
}