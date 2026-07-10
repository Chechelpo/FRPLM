package io.github.chechelpo.frplm.domain.lorebook.keywords;

import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static io.github.chechelpo.frplm.domain.EntityTypes.KEYWORDS_URL;

@RestController
@RequestMapping(KEYWORDS_URL)
final class KeywordController extends EntityController<KeywordRecord, KeywordService> {
    private final EntryKeywordService entryKeywordService;

    KeywordController(KeywordService service, EntryKeywordService entryKeywordsService) {
        super(service);
        this.entryKeywordService = entryKeywordsService;
    }

    @GetMapping
    public ResponseEntity<String[]> getAllKeywords() {
        return ResponseEntity.ok(
                service.getAll().stream()
                        .map(KeywordRecord::getKeyword)
                        .toArray(String[]::new)
        );
    }

    @Override
    protected ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) {
        throw new UnsupportedAction("Keywords can't be created via framework", Severity.USER);
    }


}
