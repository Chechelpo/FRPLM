package io.github.chechelpo.frplm.domain.lorebook;

import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import org.springframework.stereotype.Component;

@Component
public class LorebookContext {
    public final EntryService entries;
    public final LorebookService lorebooks;
    public final EntryKeywordService entryKeywords;
    public final KeywordService keywords;
    public final OutletService outlets;

    LorebookContext(
            EntryService entries,
            LorebookService lorebooks,
            EntryKeywordService entryKeywords,
            KeywordService keywords,
            OutletService outlets
    ){
        this.entries = entries;
        this.lorebooks = lorebooks;
        this.entryKeywords = entryKeywords;
        this.keywords = keywords;
        this.outlets = outlets;
    }
}
