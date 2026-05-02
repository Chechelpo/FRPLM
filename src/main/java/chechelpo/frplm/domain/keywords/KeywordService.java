package chechelpo.frplm.domain.keywords;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.stereotype.Component;

@Component
public final class KeywordService extends ABSEntityService<KeywordRecord, KeywordStore> {
    KeywordService(KeywordStore store) {
        super(store, EntityTypes.Types.KEYWORDS);
    }
}
