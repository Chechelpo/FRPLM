package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.SECTIONS_URL;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

@RestController
@RequestMapping(SECTIONS_URL)
final class SectionController extends ABSEntityController<PromptSectionRecord, SectionService> {
    SectionController(SectionService service) {
        super(service);
    }

    @GetMapping("/ofTemplate/{templateKey}")
    public ResponseEntity<EntityDTO[]> findAllByTemplateKey(@PathVariable("templateKey") Integer templateKey) {
        return ResponseEntity.ok(
                wrapEntities(
                        service.getMatching(
                                EntityKey.of(PROMPT_SECTION.PROMPT_ID, templateKey)
                        )
                )
        );
    }
}
