package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityController;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static chechelpo.frplm.domain.EntityTypes.ENTRIES_KEYWORDS_URL;
import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@RestController
@RequestMapping(ENTRIES_KEYWORDS_URL)
final class EntryKeywordsController extends EntityController<EntryKeywordsRecord, EntryKeywordService> {
    public EntryKeywordsController(EntryKeywordService service) {
        super(service);
    }

    @GetMapping("/{lorebookID}")
    public ResponseEntity<List<String>> keywordsOfLorebook(@PathVariable int lorebookID) {
        return ResponseEntity.ok(
                service.keywordsInLorebook(lorebookID)
        );
    }

    @PutMapping("/{lorebookID}/{entryID}")
    public ResponseEntity<Boolean> associate(@PathVariable int lorebookID, @PathVariable int entryID, @RequestParam String name) {
        return service.associate(lorebookID, entryID, name) ? ResponseEntity.ok(true) : ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{lorebookID}/{entryID}")
    public ResponseEntity<Boolean> dissociate(@PathVariable int lorebookID, @PathVariable int entryID, @RequestParam String name) {
        return ResponseEntity.ok(service.dissociate(lorebookID, entryID, name));
    }

    @GetMapping("/{lorebookID}/{entryID}")
    public ResponseEntity<List<String>> keywordsOfEntry(@PathVariable int lorebookID, @PathVariable int entryID) {
        return ResponseEntity.ok(
                service.keywordsOfEntry(EntityKey.<EntryRecord>builder()
                        .set(ENTRY.LOREBOOK_ID, lorebookID)
                        .set(ENTRY.ENTRY_ID, entryID)
                        .build()
                )
        );
    }

    @Override
    protected @NotNull ResponseEntity<Boolean> delete(Map<String, Object> params) {
        return ResponseEntity.badRequest().build();
    }

    @Override
    protected @NotNull ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) throws URISyntaxException {
        return ResponseEntity.badRequest().build();
    }

    @Override
    protected @NotNull ResponseEntity<EntityDTO[]> getAll(Map<String, Object> query) {
        return ResponseEntity.badRequest().build();
    }

    @Override
    protected ResponseEntity<Boolean> patch(Map<String, Object> identityParams, Map<String, Object> patch) {
        return ResponseEntity.badRequest().build();
    }

    @Override
    protected ResponseEntity<EntityDTO> get(Map<String, Object> variables) throws EntityNotFound {
        return ResponseEntity.badRequest().build();
    }
}
