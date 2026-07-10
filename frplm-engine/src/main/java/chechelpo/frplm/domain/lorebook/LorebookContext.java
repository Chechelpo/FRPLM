package chechelpo.frplm.domain.lorebook;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
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
