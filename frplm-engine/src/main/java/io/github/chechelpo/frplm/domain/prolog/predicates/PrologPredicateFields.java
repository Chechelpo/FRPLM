package io.github.chechelpo.frplm.domain.prolog.predicates;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateRecord;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;

@Component
final class PrologPredicateFields
        extends EntityFieldsValidator<PrologPredicateRecord> {

    PrologPredicateFields() {
        super(PROLOG_PREDICATE);
    }

    @Override
    protected List<FieldInfo<PrologPredicateRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(PROLOG_PREDICATE.ID)
                        .key()
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE.NAME)
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE.ARITY)
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE.KIND)
                        .addAllowedValues(
                                Arrays.stream(PrologPredicateKind.values())
                                        .map(PrologPredicateKind::getTableName)
                                        .toList()
                        )
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE.DESCRIPTION)
                        .nullable()
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE.PROVIDER_KEY)
                        .nullable()
                        .build(),

                FieldInfo.builder(PROLOG_PREDICATE.SOURCE)
                        .build()
        );
    }
}