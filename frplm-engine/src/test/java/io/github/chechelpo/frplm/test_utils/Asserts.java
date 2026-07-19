package io.github.chechelpo.frplm.test_utils;

import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class Asserts {
    private Asserts() {}

    public static <R extends TableRecord<R>> void assertEqualsMinusFields(
            R expected,
            R actual,
            Set<TableField<R, ?>> ignoreFields
    ){
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
}
