package chechelpo.frplm.domain.keywords;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.EntityTypes.KEYWORDS_URL;
import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@RestController
@RequestMapping(KEYWORDS_URL)
final class KeywordController extends ABSEntityController<KeywordRecord, KeywordService> {
    private final EntryKeywordService entryKeywordService;

    KeywordController(KeywordService service, EntryKeywordService entryKeywordsService) {
        super(EntityTypes.Types.KEYWORDS, service);
        this.entryKeywordService = entryKeywordsService;
    }

    @GetMapping( "/entry/{lorebookID}/{entryID}")
    public ResponseEntity<EntityDTO[]> getOfKeyword(@PathVariable Integer lorebookID, @PathVariable Integer entryID) {
        EntityKey.Builder<EntryRecord> key = EntityKey.builder();
        return ResponseEntity.ok(
                wrapEntities(entryKeywordService.keywordsOfEntry(
                        key
                                .set(ENTRY.LOREBOOK_ID, lorebookID)
                                .set(ENTRY.ENTRY_ID, entryID)
                                .build()
                ))
        );
    }
}
