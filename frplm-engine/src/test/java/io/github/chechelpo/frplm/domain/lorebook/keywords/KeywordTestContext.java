package io.github.chechelpo.frplm.domain.lorebook.keywords;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class KeywordTestContext {
    @Autowired
    public KeywordService service;
    @Autowired
    KeywordFieldHelper fields;
}
