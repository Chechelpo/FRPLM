package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY_KEYWORDS;

@Component
final class EntryKeywordsFieldHelper extends ABSFieldInstantiationHelper<
        EntryKeywordsRecord, EntryKeywordStore, EntryKeywordService, EntryKeywordsController
        > {
    EntryKeywordsFieldHelper(EntryKeywordStore store, EntryKeywordService service, EntryKeywordsController controller) {
        super(store, service, controller);
        register_field(
                "lorebook_id",
                ENTRY_KEYWORDS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .require()
                        .build()
        );

    }
}
