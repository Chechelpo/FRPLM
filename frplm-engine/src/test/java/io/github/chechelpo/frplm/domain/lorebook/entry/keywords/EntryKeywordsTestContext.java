package io.github.chechelpo.frplm.domain.lorebook.entry.keywords;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class EntryKeywordsTestContext {
    @Autowired
    public EntryKeywordService entryKeywordsService;
    @Autowired
    EntryKeywordsFieldHelper fields;
}
