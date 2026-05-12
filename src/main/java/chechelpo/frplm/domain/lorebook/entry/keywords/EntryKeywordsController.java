package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.ENTRIES_KEYWORDS_URL;

@RestController
@RequestMapping(ENTRIES_KEYWORDS_URL)
final class EntryKeywordsController extends ABSEntityController<EntryKeywordsRecord, EntryKeywordService> {
    public EntryKeywordsController(EntryKeywordService service) {
        super(EntityTypes.Types.ENTRY_KEYWORDS, service);
    }
}
