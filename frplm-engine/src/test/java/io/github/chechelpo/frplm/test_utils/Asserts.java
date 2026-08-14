package io.github.chechelpo.frplm.test_utils;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class Asserts {
    private Asserts() {
    }

    public static <R extends TableRecord<R>> void assertEqualsMinusFields(
            R expected,
            R actual,
            Set<TableField<R, ?>> ignoreFields
    ) {
        Arrays.stream(expected.getTable().fields())
                .filter(field -> !ignoreFields.contains(field))
                .forEach(field ->
                        assertEquals(expected.get(field), actual.get(field),
                                "%s : Mismatch in field %s".formatted(
                                        expected.getQualifier(),
                                        field.getName()
                                )
                        )
                );
    }

    @SuppressWarnings("unchecked")
    public static <R extends TableRecord<R>> void assertRecordEqualsPayloadMinusFields(
            R record,
            EntityDataPayload<R> dataPayload,
            Set<TableField<R, ?>> ignoreFields
    ) {
        Arrays.stream(record.getTable().fields())
                .map(field -> (TableField<R, Object>) field)
                .filter(field -> !ignoreFields.contains(field))
                .forEach(field -> {
                            assertTrue(dataPayload.assigns(field), "Field " + field.getName() + " is not assigned by creationPayload");
                            assertEquals(record.get(field), dataPayload.require(field),
                                    "%s : Mismatch in field %s".formatted(
                                            record.getQualifier(),
                                            field.getName()
                                    )
                            );
                        }
                );
    }

    public static  <R extends TableRecord<R>, O extends TableRecord<O>> void assertEqualsTwoRecords(
            R expected,
            O actual,
            Map<TableField<R, ?>, TableField<O, ?>> compareFields
    ) {
        compareFields.entrySet().stream()
                .filter(field -> !compareFields.containsKey(field.getKey()))
                .forEach(
                        fieldPair -> {
                            assertEquals(
                                    expected.get(fieldPair.getKey()),
                                    actual.get(fieldPair.getValue()),
                                    """
                                            Mismatch on field %s vs %s
                                            """.formatted(fieldPair.getKey(), fieldPair.getValue())
                            );
                        }
                );
    }


}
