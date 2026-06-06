package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class EntryKeywordsTestContext {
    @Autowired
    public EntryKeywordService entryKeywordsService;
    @Autowired
    EntryKeywordsFieldHelper fields;
}
