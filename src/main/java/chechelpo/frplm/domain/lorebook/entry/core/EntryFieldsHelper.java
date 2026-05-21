package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.frameworks.entities.fields.constraints.BoolConstraints;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.frameworks.entities.fields.CommonFields;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.format.NumberCoercer;
import chechelpo.frplm.frameworks.entities.fields.format.StringCoercer;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@Component
final class EntryFieldsHelper extends ABSControllerAwareHelper<
        EntryRecord,
        EntryService,
        EntryController
        > {

    EntryFieldsHelper(
            EntryService service,
            EntryController controller
    ) {
        super(service, controller);
        // Key
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

        // Data
        register_field(
                CommonFields.NAME.getFieldName(),
                Entry.ENTRY.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .build()
        );
        register_field(
                "enabled",
                ENTRY.ENABLED,
                FieldInfo.booleanField()
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
                        .build()
        );

        // Injection requirements
        register_field(
                "probability",
                Entry.ENTRY.PROBABILITY,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.SHORT)
                                        .build()
                        )
                        .build()
        );


        register_field(
                "outlet_id",
                Entry.ENTRY.OUTLET,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .nullable()
                                        .build()
                        )
                        .build()
        );

        register_field(
                "delay",
                Entry.ENTRY.DELAY,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );

        register_field(
                "cooldown",
                Entry.ENTRY.COOLDOWN,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );
        register_field(
                "stick_through",
                Entry.ENTRY.STICK_THROUGH,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );

        // Injection Options
        register_field(
                "injection_order",
                Entry.ENTRY.INJECTION_ORDER,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER).build())
                        .build()
        );

        //Activation strategy
        register_field(
                "strategy",
                Entry.ENTRY.STRATEGY,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.SHORT)
                                        .setPossibleValues(ActivationStrategy.stableIDs())
                                        .build()
                        )
                        .build(),
                ActivationStrategy.COMMON.stable_id
        );
        register_field(
                "embed_text",
                Entry.ENTRY.EMBED_TEXT,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder().build()
                        )
                        .build()
        );
        register_field(
                "prevent_further_recursion",
                Entry.ENTRY.PREVENT_FURTHER_RECURSION,
                FieldInfo.booleanField()
                        .setConstraints(BoolConstraints.builder())
                        .build()
        );
        register_field(
                "non_recursable",
                Entry.ENTRY.NON_RECURSABLE,
                FieldInfo.booleanField()
                        .setConstraints(BoolConstraints.builder())
                        .build()
        );
        register_field(
                "delay_until_recursion",
                Entry.ENTRY.DELAY_UNTIL_RECURSION,
                FieldInfo.booleanField()
                        .setConstraints(BoolConstraints.builder())
                        .build()
        );
        register_field(
                "scan_depth",
                Entry.ENTRY.SCAN_DEPTH,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.SHORT)
                                        .setMin((long) Short.MAX_VALUE)
                                        .setMin(0L)
                                        .build()
                        )
                        .build()
        );
    }
}
