package io.github.chechelpo.frplm.test_utils.comparators;

import org.jooq.TableField;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class RecordComparator<E extends TableRecord<E>, A extends TableRecord<A>> {
    private final E expected;
    private final A actual;
    private final List<Comparison<E,A,?>> comparisons = new ArrayList<>();
    private String message;

    interface Comparison<E extends TableRecord<E>, A extends TableRecord<A>, T>{
        void assertComparison(E expected, A actual);

        record EqualsValue<E extends TableRecord<E>, A extends TableRecord<A>, T>(
                TableField<E,T> field,
                T expectedValue,
                boolean isEqual
        ) implements Comparison<E,A,T> {
            @Override
            public void assertComparison(E expected, A ignored){
                if (isEqual)
                    Assertions.assertEquals(
                            expectedValue,
                            expected.get(field),
                            "Mismatch in value of %s for \n%s".formatted(field.getName(), expected)
                    );
                else
                    Assertions.assertNotEquals(
                            expectedValue,
                            expected.get(field),
                            "Mismatch in value of %s for \n%s".formatted(field.getName(), expected)
                    );
            }
        }

        record Equals<E extends TableRecord<E>, A extends TableRecord<A>, T>(
                TableField<E, T> expectedField,
                TableField<A, T> actualField,
                boolean isEqual
        ) implements Comparison<E,A,T> {
            @Override
            public void assertComparison(E expected, A actual) {
                if (isEqual)
                    Assertions.assertEquals(
                            expected.get(expectedField),
                            actual.get(actualField),
                            "Mismatch on field %s equals %s on\n%s\nvs\n%s".formatted(expectedField, actualField, expected, actual)
                    );
                else
                    Assertions.assertNotEquals(
                            expected.get(expectedField),
                            actual.get(actualField),
                            "Mismatch on field %s equals %s on\n%s\nvs\n%s".formatted(expectedField, actualField, expected, actual)
                    );
            }
        }
    }

    private RecordComparator(E expected, A actual) {
        this.expected = expected;
        this.actual = actual;
    }

    public static <E extends TableRecord<E>, A extends TableRecord<A>> RecordComparator<E, A> compare(
            E expected,
            A actual
    ) {
        return new RecordComparator<>(expected, actual);
    }

    public <T> RecordComparator<E,A> equals(TableField<E,T> expectedField, T expectedValue){
        comparisons.add(new Comparison.EqualsValue<>(expectedField, expectedValue, true));
        return this;
    }
    public <T> RecordComparator<E,A> equals(TableField<E,T> expectedField, TableField<A,T> actualField) {
        comparisons.add(new Comparison.Equals<>(expectedField, actualField, true));
        return this;
    }

    public <T> RecordComparator<E,A> notEquals(TableField<E,T> expectedField, T expectedValue){
        comparisons.add(new Comparison.EqualsValue<>(expectedField, expectedValue, false));
        return this;
    }
    public <T> RecordComparator<E,A> notEquals(TableField<E,T> expectedField, TableField<A,T> actualField) {
        comparisons.add(new Comparison.Equals<>(expectedField, actualField, false));
        return this;
    }
    public void execute(){
        comparisons.forEach(
                comparison -> comparison.assertComparison(expected, actual)
        );
    }
}