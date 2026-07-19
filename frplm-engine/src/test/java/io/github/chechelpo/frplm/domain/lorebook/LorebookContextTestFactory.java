package io.github.chechelpo.frplm.domain.lorebook;

import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;

public final class LorebookContextTestFactory {

    private LorebookContextTestFactory() {
    }

    public static LorebookContext create(
            EntryService entries,
            LorebookService lorebooks,
            EntryKeywordService entryKeywords,
            KeywordService keywords,
            OutletService outlets
    ) {
        return new LorebookContext(
                entries,
                lorebooks,
                entryKeywords,
                keywords,
                outlets
        );
    }
}