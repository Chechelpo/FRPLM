package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY_KEYWORDS;

@Component
final class EntryKeywordsFieldHelper extends ABSControllerAwareHelper<
        EntryKeywordsRecord, EntryKeywordService, EntryKeywordsController
        > {
    EntryKeywordsFieldHelper(EntryKeywordService service, EntryKeywordsController controller) {
        super(service, controller);
        register_field(
                "lorebook_id",
                ENTRY_KEYWORDS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .require()
                        .build()
        );
        register_field(
                "entry_id",
                ENTRY_KEYWORDS.ENTRY_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .require()
                        .build()
        );
        register_field(
                "keyword_id",
                ENTRY_KEYWORDS.KEYWORD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .require()
                        .build()
        );

    }
}
