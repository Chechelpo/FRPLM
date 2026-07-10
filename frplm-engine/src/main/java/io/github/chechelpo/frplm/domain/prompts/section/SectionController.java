package io.github.chechelpo.frplm.domain.prompts.section;

import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.SECTIONS_URL;

@RestController
@RequestMapping(SECTIONS_URL)
final class SectionController extends EntityController<PromptSectionRecord, SectionService> {
    SectionController(SectionService service) {
        super(service);
    }

    @GetMapping("/ofTemplate/{templateKey}")
    public ResponseEntity<EntityDTO[]> findAllByTemplateKey(@PathVariable("templateKey") short templateKey) {
        return ResponseEntity.ok(
                wrapEntities(
                        service.getMatching(
                                EntityKey.of(PROMPT_SECTION.PROMPT_ID, templateKey)
                        )
                )
        );
    }

    @PostMapping("/exchange/{promptID}/{sectionID1}/{sectionID2}")
    public ResponseEntity<Boolean> exchange(
            @PathVariable short promptID,
            @PathVariable short sectionID1,
            @PathVariable short sectionID2
    ) throws EntityNotFound {
        boolean exchanged = service.exchange(promptID, sectionID1, sectionID2);

        if (!exchanged) {
            return ResponseEntity.badRequest().body(false);
        }

        return ResponseEntity.ok(true);
    }
}
