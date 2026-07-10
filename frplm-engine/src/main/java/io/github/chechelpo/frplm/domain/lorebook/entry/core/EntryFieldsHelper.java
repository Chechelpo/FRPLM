package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.core.entities.fields.constraints.BoolConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.Entry;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.core.entities.fields.CommonFields;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

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
                                NumberConstraint.builder(FieldType.INTEGER)
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
                                NumberConstraint.builder(FieldType.INTEGER)
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
                                StringConstraint.builder()
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
                                StringConstraint.builder()
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
                                NumberConstraint.builder(FieldType.SHORT)
                                        .build()
                        )
                        .build()
        );


        register_field(
                "outlet_id",
                Entry.ENTRY.OUTLET,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .nullable()
                                        .build()
                        )
                        .build()
        );

        register_field(
                "delay",
                Entry.ENTRY.DELAY,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );

        register_field(
                "cooldown",
                Entry.ENTRY.COOLDOWN,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );
        register_field(
                "stick_through",
                Entry.ENTRY.STICK_THROUGH,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );

        // Injection Options
        register_field(
                "injection_order",
                Entry.ENTRY.POSITION,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                                .build()
                        )
                        .build()
        );

        //Activation strategy
        register_field(
                "strategy",
                Entry.ENTRY.STRATEGY,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.SHORT)
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
                                StringConstraint.builder()
                                        .nullable()
                                        .build()
                        )
                        .build()
        );
        register_field(
                "prevent_further_recursion",
                Entry.ENTRY.PREVENT_FURTHER_RECURSION,
                FieldInfo.booleanField()
                        .setConstraints(BoolConstraint.builder())
                        .build()
        );
        register_field(
                "non_recursable",
                Entry.ENTRY.NON_RECURSABLE,
                FieldInfo.booleanField()
                        .setConstraints(BoolConstraint.builder())
                        .build()
        );
        register_field(
                "delay_until_recursion",
                Entry.ENTRY.DELAY_UNTIL_RECURSION,
                FieldInfo.booleanField()
                        .setConstraints(BoolConstraint.builder())
                        .build()
        );
        register_field(
                "scan_depth",
                Entry.ENTRY.SCAN_DEPTH,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.SHORT)
                                        .nullable()
                                        .setMin((long) Short.MAX_VALUE)
                                        .setMin(0L)
                                        .build()
                        )
                        .build()
        );
    }
}
