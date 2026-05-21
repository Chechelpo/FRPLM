package chechelpo.frplm.domain.prompts.section.utils;

import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.domain.prompts.section.SectionService;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
final class PromptSectionRepository extends EntityRepository<PromptSectionRecord, SectionService> {
    private final OutletService outletService;
    PromptSectionRepository(SectionService service, KeywordService keywords, OutletService outletService) {
        super(service);
        this.outletService = outletService;
    }

    Optional<Integer> getOutletID(String outletName) {
        return outletService.getOutletID(outletName);
    }
}
