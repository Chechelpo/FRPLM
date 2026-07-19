package io.github.chechelpo.frplm.utils.prolog;

import it.unibo.tuprolog.core.Clause;
import it.unibo.tuprolog.core.Struct;
import it.unibo.tuprolog.core.parsing.ParseException;
import it.unibo.tuprolog.core.parsing.TermParser;
import it.unibo.tuprolog.theory.parsing.ClausesParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public final class PrologSyntaxValidator implements PrologSourceValidator {
    private static final TermParser TERM_PARSER =
            TermParser.withDefaultOperators();

    private static final ClausesParser THEORY_PARSER =
            ClausesParser.withDefaultOperators();

    public PrologSyntaxValidator(){}

    @Override
    public ValidationResult validate(String source) {
        if (source == null || source.isBlank()) {
            return ValidationResult.error(
                    ErrorType.UNKNOWN_TERM,
                    null,
                    "Source cannot be empty",
                    1,
                    1
            );
        }

        try {
            List<Clause> clauses = THEORY_PARSER.parseClauses(source);

            if (clauses.isEmpty()) {
                return ValidationResult.error(
                        ErrorType.UNKNOWN_TERM,
                        null,
                        "Expected a predicate definition",
                        1,
                        1
                );
            }

            for (Clause clause : clauses) {
                Struct head = clause.getHead();

                if (head == null || !isUserPredicate(head)) {
                    return ValidationResult.error(
                            ErrorType.UNKNOWN_TERM,
                            head == null
                                    ? null
                                    : head.getFunctor(),
                            "Expected a predicate fact or rule",
                            1,
                            1
                    );
                }

                if (!clause.isWellFormed()) {
                    return ValidationResult.error(
                            ErrorType.BAD_SYNTAX,
                            head.getFunctor(),
                            "Malformed predicate definition",
                            1,
                            1
                    );
                }
            }

            return ValidationResult.success();

        } catch (ParseException exception) {
            return ValidationResult.error(
                    ErrorType.BAD_SYNTAX,
                    exception.getOffendingSymbol(),
                    exception.getMessage(),
                    exception.getLine(),
                    exception.getColumn()
            );
        }
    }

    private boolean isUserPredicate(Struct head) {
        /*
         * Accepts conventional predicate names:
         *
         * parent.
         * parent(X, Y).
         * ancestor(X, Y) :- parent(X, Y).
         *
         * Rejects strings, lists, numbers, operators, etc.
         */
        return head.getFunctor()
                .matches("[a-z][A-Za-z0-9_]*");
    }
}