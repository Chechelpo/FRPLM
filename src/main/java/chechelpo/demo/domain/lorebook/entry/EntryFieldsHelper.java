package chechelpo.demo.domain.lorebook.entry;

import chechelpo.demo.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.demo.jooq.generated.tables.Entry;
import chechelpo.demo.jooq.generated.tables.records.EntryRecord;
import chechelpo.demo.frameworks.entities.fields.CommonFields;
import chechelpo.demo.frameworks.entities.fields.FieldInfo;
import chechelpo.demo.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.demo.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.demo.frameworks.entities.fields.format.NumberFormat;
import chechelpo.demo.frameworks.entities.fields.format.StringFormat;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class EntryFieldsHelper extends ABSFieldInstantiationHelper<
        EntryRecord,
        EntryStore,
        EntryService,
        EntryController
        > {

    EntryFieldsHelper(
            EntryService service,
            EntryStore store,
            EntryController controller
    ) {
        super(store, service, controller);
        register_field(
                "lorebook_id",
                Entry.ENTRY.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .require()
                        .build()
        );
        register_field(
                "entry_id",
                Entry.ENTRY.ENTRY_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .build()
        );


        register_field(
                CommonFields.NAME.getFieldName(),
                Entry.ENTRY.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .setFormat(
                                StringFormat.builder()
                                        .setInfo("Metadata name")
                                        .setType(StringFormat.Types.SHORT_TEXT)
                                        .build()
                        )
                        .build()
        );

        register_field(
                "content",
                Entry.ENTRY.CONTENT,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
                                        .allows_outlets()
                                        .build()
                        )
                        .setFormat(
                                StringFormat.builder()
                                        .setType(StringFormat.Types.LONG_TEXT)
                                        .build()
                        )
                        .build()
        );

        register_field(
                "probability",
                Entry.ENTRY.PROBABILITY,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.SHORT)
                                        .build()
                        )
                        .setFormat(
                                NumberFormat.builder()
                                        .setInfo("The probability of this content being inputted on a successful activation")
                                        .setType(NumberFormat.Types.INPUT)
                                        .build()
                        )
                        .build()
        );

        register_field(
                "strategy",
                Entry.ENTRY.STRATEGY,
                FieldInfo.stringField()
                        .build()
        );


        register_field(
                "scan_depth",
                Entry.ENTRY.SCAN_DEPTH,
                FieldInfo.numberField(FieldType.SHORT).build()
        );
    }
}
