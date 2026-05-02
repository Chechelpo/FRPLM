package chechelpo.frplm.domain.keywords;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.KEYWORD;

@Component
final class KeywordStore extends ABSEntityStore<KeywordRecord> {
    KeywordStore(@NotNull DSLContext ctx) {
        super(ctx, KEYWORD, EntityTypes.Types.KEYWORDS);
    }
}
