package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.UnsupportedAction;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.Map;

import static chechelpo.frplm.domain.EntityTypes.KEYWORDS_URL;
import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@RestController
@RequestMapping(KEYWORDS_URL)
final class KeywordController extends EntityController<KeywordRecord, KeywordService> {
    private final EntryKeywordService entryKeywordService;

    KeywordController(KeywordService service, EntryKeywordService entryKeywordsService) {
        super(service);
        this.entryKeywordService = entryKeywordsService;
    }

    @Override
    protected ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) throws URISyntaxException {
        throw new UnsupportedAction("Keywords can't be created via framework", Severity.USER);
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
