package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.core.entities.fields.constraints.IntegerConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.ShortConstraint;
import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.Entry;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.core.entities.fields.CommonFields;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

@Component
final class EntryFieldsHelper extends EntityControllerFieldValidator<EntryRecord> {
    EntryFieldsHelper() {
        super(EntityConfigs.Types.ENTRIES);
    }

    @Override
    protected List<DTOField<EntryRecord, ?>> getDTOStructure() {
        return List.of(
                // Key
                DTOField.of(Entry.ENTRY.LOREBOOK_ID, "lorebook_id"),
                DTOField.of(Entry.ENTRY.ENTRY_ID, "entry_id"),

                // Data
                DTOField.of(Entry.ENTRY.NAME, CommonFields.NAME.getFieldName()),
                DTOField.of(ENTRY.ENABLED, "enabled"),
                DTOField.of(Entry.ENTRY.CONTENT, "content"),

                // Injection requirements
                DTOField.of(Entry.ENTRY.PROBABILITY, "probability"),
                DTOField.of(Entry.ENTRY.OUTLET, "outlet_id"),
                DTOField.of(Entry.ENTRY.DELAY, "delay"),
                DTOField.of(Entry.ENTRY.COOLDOWN, "cooldown"),
                DTOField.of(Entry.ENTRY.STICK_THROUGH, "stick_through"),

                // Injection Options
                DTOField.of(Entry.ENTRY.POSITION, "injection_order"),

                // Activation strategy
                DTOField.of(Entry.ENTRY.STRATEGY, "strategy"),
                DTOField.of(Entry.ENTRY.EMBED_TEXT, "embed_text"),
                DTOField.of(Entry.ENTRY.PREVENT_FURTHER_RECURSION, "prevent_further_recursion"),
                DTOField.of(Entry.ENTRY.NON_RECURSABLE, "non_recursable"),
                DTOField.of(Entry.ENTRY.DELAY_UNTIL_RECURSION, "delay_until_recursion"),
                DTOField.of(Entry.ENTRY.SCAN_DEPTH, "scan_depth")
        );
    }

    @Override
    protected List<FieldInfo<EntryRecord, ?>> getCustom() {
        return List.of(
                // Key
                FieldInfo.builder(Entry.ENTRY.LOREBOOK_ID)
                        .key()
                        .readOnly()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(Entry.ENTRY.ENTRY_ID)
                        .key()
                        .readOnly()
                        .build(),

                // Data
                FieldInfo.builder(Entry.ENTRY.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .build(),

                FieldInfo.builder(Entry.ENTRY.CONTENT)
                        .build(),

                // Injection requirements
                FieldInfo.builder(Entry.ENTRY.OUTLET)
                        .nullable()
                        .build(),

                FieldInfo.builder(Entry.ENTRY.DELAY)
                        .nullable()
                        .setConstraints(
                                IntegerConstraint.builder()
                                    .setMin(0)
                                    .build()
                        )
                        .build(),

                FieldInfo.builder(Entry.ENTRY.COOLDOWN)
                        .nullable()
                        .setConstraints(
                                IntegerConstraint.builder()
                                .setMin(0)
                                .build()
                        )
                        .build(),

                FieldInfo.builder(Entry.ENTRY.STICK_THROUGH)
                        .nullable()
                        .setConstraints(
                                IntegerConstraint.builder()
                                        .setMin(0)
                                        .build()
                        )
                        .build(),

                // Injection Options
                FieldInfo.builder(Entry.ENTRY.POSITION)
                        .nullable()
                        .build(),

                // Activation strategy
                FieldInfo.builder(Entry.ENTRY.STRATEGY)
                        .addAllowedValues(ActivationStrategy.stableIDs())
                        .setDefaultValue(ActivationStrategy.COMMON.stable_id)
                        .build(),

                FieldInfo.builder(Entry.ENTRY.EMBED_TEXT)
                        .nullable()
                        .build(),

                // GROUP_ID — no DTO name, internal only
                FieldInfo.builder(ENTRY.GROUP_ID)
                        .nullable()
                        .build(),

                FieldInfo.builder(Entry.ENTRY.SCAN_DEPTH)
                        .nullable()
                        .setConstraints(
                                ShortConstraint.builder()
                                        .setMin((short) 0)
                                        .build()
                        )
                        .build()
        );
    }
}