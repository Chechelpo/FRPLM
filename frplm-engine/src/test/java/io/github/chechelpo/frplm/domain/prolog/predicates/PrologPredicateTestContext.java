package io.github.chechelpo.frplm.domain.prolog.predicates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class PrologPredicateTestContext {
    @Autowired
    public PrologPredicateService service;

    @Autowired
    PrologPredicateFields fields;
}
