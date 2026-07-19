package io.github.chechelpo.frplm.domain.prolog;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateArgumentRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PrologPredicateRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE;
import static io.github.chechelpo.frplm.jooq.generated.Tables.PROLOG_PREDICATE_ARGUMENT;

@Component
public class PrologService {
    /**
     * Extracts:
     * <p>
     *     1. Arity
     * </p>
     * @param ruleSource raw text of prolog
     */
    public void createPrologRule(String ruleSource){

    }
}
