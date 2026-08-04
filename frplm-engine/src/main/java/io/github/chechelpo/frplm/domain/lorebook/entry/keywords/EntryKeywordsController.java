package io.github.chechelpo.frplm.domain.lorebook.entry.keywords;

import io.github.chechelpo.frplm.core.entities.pseudo_services.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDTO;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.ENTRIES_KEYWORDS_URL;

@RestController
@RequestMapping(ENTRIES_KEYWORDS_URL)
final class EntryKeywordsController extends EntityController<EntryKeywordsRecord, EntryKeywordService> {
    public EntryKeywordsController(EntryKeywordService service, DTOMapper<EntryKeywordsRecord> mapper) {
        super(service, mapper);
    }

    @GetMapping("/{lorebookID}")
    public ResponseEntity<List<String>> keywordsOfLorebook(@PathVariable int lorebookID) {
        return ResponseEntity.ok(
                service.keywordsOfLorebook(lorebookID).stream().toList()
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
                service.keywordsOfEntry(lorebookID, entryID).stream().toList()
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
    protected ResponseEntity<Boolean> patch(Map<String, Object> identityParams, Map<String, Object> patch) {
        return ResponseEntity.badRequest().build();
    }

    @Override
    protected ResponseEntity<EntityDTO> get(Map<String, Object> variables) throws EntityNotFound {
        return ResponseEntity.badRequest().build();
    }
}
