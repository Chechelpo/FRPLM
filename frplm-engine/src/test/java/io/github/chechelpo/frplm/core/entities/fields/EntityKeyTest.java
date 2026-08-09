package io.github.chechelpo.frplm.core.entities.fields;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.ExpectedField;
import org.jooq.Condition;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityKeyTest {

    private interface Rec extends TableRecord<Rec> {
    }

    @SuppressWarnings("unchecked")
    private static <T> TableField<Rec, T> field(
            String name,
            Class<T> type
    ) {
        TableField<Rec, T> field = mock(TableField.class);

        when(field.getName()).thenReturn(name);
        when(field.getType()).thenReturn(type);
        when(field.toString()).thenReturn(name);

        return field;
    }

    private static Condition condition(String rendered) {
        Condition condition = mock(Condition.class);
        when(condition.toString()).thenReturn(rendered);
        return condition;
    }

    @Test
    void builderWithoutAssignmentsCreatesEmptyKey() {
        EntityKey<Rec> key = EntityKey.<Rec>builder().build();

        assertTrue(key.isEmpty());
        assertTrue(key.assignments().isEmpty());
        assertEquals(0, key.getEqualityConditions().length);
        assertNotNull(key.getPkCondition());
    }

    @Test
    void builderSetAddsSingleValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .build();

        assertFalse(key.isEmpty());
        assertEquals(1, key.assignments().size());
        assertTrue(key.assigns(id));
        assertEquals(10, key.require(id));
    }

    @Test
    void staticBuilderWithFieldAndValueAddsInitialValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.builder(id, 10).build();

        assertEquals(1, key.assignments().size());
        assertEquals(10, key.require(id));
    }

    @Test
    void staticOfWithoutArgumentsCreatesEmptyKey() {
        EntityKey<Rec> key = EntityKey.of();

        assertTrue(key.isEmpty());
        assertTrue(key.assignments().isEmpty());
    }

    @Test
    void staticOfCreatesKeyWithSingleValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertEquals(1, key.assignments().size());
        assertEquals(10, key.require(id));
    }

    @SuppressWarnings("deprecation")
    @Test
    void ofValuesCreatesKeyFromMap() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 10);
        values.put(name, "Alice");

        EntityKey<Rec> key = EntityKey.ofValues(values);

        assertEquals(2, key.assignments().size());
        assertEquals(10, key.require(id));
        assertEquals("Alice", key.require(name));
    }

    @Test
    void builderSetAllAddsAllValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 10);
        values.put(name, "Alice");

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .setAll(values)
                .build();

        assertEquals(2, key.assignments().size());
        assertEquals(10, key.require(id));
        assertEquals("Alice", key.require(name));
    }

    @Test
    void builderUnsafeSetAddsValueWithoutTypeCheck() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .unsafeSet(id, "not-an-integer")
                .build();

        assertEquals("not-an-integer", key.assignments().get(id));
    }

    @Test
    void builderSetCanOverwriteValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, "Alice")
                .set(name, "Bob")
                .build();

        assertEquals(1, key.assignments().size());
        assertEquals("Bob", key.require(name));
    }

    @Test
    void builderSetAllCanOverwriteExistingValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Bob");

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, "Alice")
                .setAll(values)
                .build();

        assertEquals(1, key.assignments().size());
        assertEquals("Bob", key.require(name));
    }

    @Test
    void assignsReturnsTrueForAssignedField() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertTrue(key.assigns(id));
        assertTrue(key.assignsField(id));
    }

    @Test
    void assignsReturnsFalseForUnassignedField() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of();

        assertFalse(key.assigns(id));
        assertFalse(key.assignsField(id));
    }

    @Test
    void getAssignmentReturnsAssignedAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        DataPayload.Assignment<Rec, Integer> assignment =
                key.getAssignment(id);

        assertTrue(assignment.isAssigned());
        assertFalse(assignment.isUnassigned());
        assertSame(id, assignment.field);
        assertEquals(10, assignment.get());
        assertEquals(10, assignment.orElse(20));
    }

    @Test
    void getAssignmentReturnsUnassignedAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of();

        DataPayload.Assignment<Rec, Integer> assignment =
                key.getAssignment(id);

        assertFalse(assignment.isAssigned());
        assertTrue(assignment.isUnassigned());
        assertSame(id, assignment.field);
        assertEquals(20, assignment.orElse(20));
    }

    @Test
    void assignedNullIsStillConsideredAssigned() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        DataPayload.Assignment<Rec, String> assignment =
                key.getAssignment(name);

        assertTrue(assignment.isAssigned());
        assertFalse(assignment.isUnassigned());
        assertNull(assignment.get());
        assertNull(assignment.orElse("default"));
        assertTrue(key.assigns(name));
    }

    @Test
    void assignmentGetThrowsForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        DataPayload.Assignment<Rec, String> assignment =
                DataPayload.Assignment.ofUnassigned(name);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                assignment::get
        );

        assertTrue(exception.getMessage().contains("NAME"));
        assertTrue(exception.getMessage().contains("has no assignment"));
    }

    @Test
    void assignmentMapTransformsAssignedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofAssigned(id, 10);

        Optional<String> mapped = assignment.map(value -> "ID-" + value);

        assertEquals(Optional.of("ID-10"), mapped);
    }

    @Test
    void assignmentMapReturnsEmptyForUnassignedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofUnassigned(id);

        Optional<String> mapped = assignment.map(value -> "ID-" + value);

        assertTrue(mapped.isEmpty());
    }

    @Test
    void assignmentOrElseThrowReturnsAssignedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofAssigned(id, 10);

        int value = assignment.orElseThrow(
                ignored -> new NoSuchElementException()
        );

        assertEquals(10, value);
    }

    @Test
    void assignmentOrElseThrowUsesExceptionProviderWhenUnassigned() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofUnassigned(id);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> assignment.orElseThrow(
                        field -> new NoSuchElementException(
                                "Missing " + field.getName()
                        )
                )
        );

        assertEquals("Missing ID", exception.getMessage());
    }

    @Test
    void assignmentDefaultOrElseThrowThrowsExpectedField() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofUnassigned(id);

        ExpectedField exception = assertThrows(
                ExpectedField.class,
                assignment::orElseThrow
        );

        assertTrue(exception.getMessage().contains("ID"));
        assertTrue(exception.getMessage().contains("is not assigned"));
    }

    @Test
    void assignmentOrElseThrowAcceptsCustomMessage() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofUnassigned(id);

        ExpectedField exception = assertThrows(
                ExpectedField.class,
                () -> assignment.orElseThrow("Custom missing-field message")
        );

        assertTrue(
                exception.getMessage().contains("Custom missing-field message")
        );
    }

    @Test
    void assignmentOrElseThrowAcceptsSeverity() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        DataPayload.Assignment<Rec, Integer> assignment =
                DataPayload.Assignment.ofUnassigned(id);

        assertThrows(
                ExpectedField.class,
                () -> assignment.orElseThrow(Severity.USER)
        );
    }

    @Test
    void requireReturnsAssignedValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.of(name, "Alice");

        assertEquals("Alice", key.require(name));
    }

    @Test
    void requireThrowsExpectedFieldForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.of();

        ExpectedField exception = assertThrows(
                ExpectedField.class,
                () -> key.require(name)
        );

        assertTrue(exception.getMessage().contains("NAME"));
        assertTrue(exception.getMessage().contains("is not assigned"));
    }

    @Test
    void requireReturnsNullForAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        assertNull(key.require(name));
    }

    @Test
    void requireNonNullReturnsAssignedNonNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.of(name, "Alice");

        assertEquals("Alice", key.requireNonNull(name));
    }

    @Test
    void requireNonNullThrowsForAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        assertThrows(
                NullPointerException.class,
                () -> key.requireNonNull(name)
        );
    }

    @Test
    void getReturnsAssignedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertEquals(Optional.of(10), key.get(id));
    }

    @Test
    void getReturnsEmptyForUnassignedField() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of();

        assertTrue(key.get(id).isEmpty());
    }

    @Test
    void getReturnsEmptyForAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        assertTrue(key.get(name).isEmpty());
        assertTrue(key.assigns(name));
    }

    @Test
    void consumeAddsAssignedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of();

        key.consume(DataPayload.Assignment.ofAssigned(id, 10));

        assertTrue(key.assigns(id));
        assertEquals(10, key.require(id));
    }

    @Test
    void consumeAddsAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.of();

        key.consume(DataPayload.Assignment.ofAssigned(name, null));

        assertTrue(key.assigns(name));
        assertNull(key.require(name));
    }

    @Test
    void consumeIgnoresUnassignedAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of();

        key.consume(DataPayload.Assignment.ofUnassigned(id));

        assertFalse(key.assigns(id));
        assertTrue(key.isEmpty());
    }

    @Test
    void consumeOverwritesExistingValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        key.consume(DataPayload.Assignment.ofAssigned(id, 20));

        assertEquals(20, key.require(id));
    }

    @Test
    void consumeIfAbsentAddsValueWhenFieldIsMissing() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of();

        key.consumeIfAbsent(DataPayload.Assignment.ofAssigned(id, 10));

        assertEquals(10, key.require(id));
    }

    @Test
    void consumeIfAbsentDoesNotOverwriteExistingValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        key.consumeIfAbsent(DataPayload.Assignment.ofAssigned(id, 20));

        assertEquals(10, key.require(id));
    }

    @Test
    void consumeIfAbsentPreservesExistingAssignedNull() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        key.consumeIfAbsent(
                DataPayload.Assignment.ofAssigned(name, "Alice")
        );

        assertTrue(key.assigns(name));
        assertNull(key.require(name));
    }

    @Test
    void assignmentsReturnsMutableBackingMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.of();

        key.assignments().put(name, "Alice");

        assertEquals("Alice", key.require(name));
    }

    @Test
    void constructorDoesNotCopyInputMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Alice");

        EntityKey<Rec> key = new EntityKey<>(values, false);

        values.put(name, "Bob");

        assertEquals("Bob", key.require(name));
    }

    @Test
    void builderBuildDoesNotCopyBuilderMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey.Builder<Rec> builder = EntityKey.<Rec>builder()
                .set(name, "Alice");

        EntityKey<Rec> key = builder.build();

        builder.set(name, "Bob");

        assertEquals("Bob", key.require(name));
    }

    @Test
    void toRecordCopiesAllAssignments() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .set(name, "Alice")
                .build();

        Rec record = mock(Rec.class);

        Rec result = key.toRecord(() -> record);

        assertSame(record, result);
        verify(record).set(id, 10);
        verify(record).set(name, "Alice");
    }

    @Test
    void toRecordCopiesAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        Rec record = mock(Rec.class);

        key.toRecord(() -> record);

        verify(record).set(name, null);
    }

    @Test
    void getEqualityConditionsUsesEqForNonNullValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        Condition eqCondition = condition("ID = 10");

        when(id.eq(10)).thenReturn(eqCondition);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        Condition[] conditions = key.getEqualityConditions();

        assertEquals(1, conditions.length);
        assertSame(eqCondition, conditions[0]);

        verify(id).eq(10);
        verify(id, never()).isNull();
    }

    @Test
    void getEqualityConditionsUsesIsNullForNullValues() {
        TableField<Rec, String> name = field("NAME", String.class);
        Condition isNullCondition = condition("NAME is null");

        when(name.isNull()).thenReturn(isNullCondition);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        Condition[] conditions = key.getEqualityConditions();

        assertEquals(1, conditions.length);
        assertSame(isNullCondition, conditions[0]);

        verify(name).isNull();
        verify(name, never()).eq(nullable(String.class));
    }

    @Test
    void getEqualityConditionsReturnsOneConditionPerAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Condition idCondition = condition("ID = 10");
        Condition nameCondition = condition("NAME = Alice");

        when(id.eq(10)).thenReturn(idCondition);
        when(name.eq("Alice")).thenReturn(nameCondition);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .set(name, "Alice")
                .build();

        Condition[] conditions = key.getEqualityConditions();

        assertEquals(2, conditions.length);
        assertTrue(
                (conditions[0] == idCondition
                        && conditions[1] == nameCondition)
                        || (conditions[0] == nameCondition
                        && conditions[1] == idCondition)
        );
    }

    @Test
    void getPkConditionReturnsNonNullConditionForEmptyKey() {
        EntityKey<Rec> key = EntityKey.of();

        assertNotNull(key.getPkCondition());
    }

    @Test
    void getPkConditionIncludesSingleAssignmentCondition() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        Condition eqCondition = condition("ID = 10");

        when(id.eq(10)).thenReturn(eqCondition);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        Condition pkCondition = key.getPkCondition();

        assertNotNull(pkCondition);
        verify(id).eq(10);
    }

    @Test
    void equalsReturnsTrueForSameInstance() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertEquals(key, key);
    }

    @Test
    void equalsReturnsFalseForNull() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertNotEquals(null, key);
    }

    @Test
    void equalsReturnsFalseForDifferentClass() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertNotEquals("not-a-key", key);
    }

    @Test
    void equalsReturnsTrueForSameValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> first = EntityKey.<Rec>builder()
                .set(id, 10)
                .set(name, "Alice")
                .build();

        EntityKey<Rec> second = EntityKey.<Rec>builder()
                .set(id, 10)
                .set(name, "Alice")
                .build();

        assertEquals(first, second);
        assertEquals(second, first);
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> first = EntityKey.of(id, 10);
        EntityKey<Rec> second = EntityKey.of(id, 20);

        assertNotEquals(first, second);
    }

    @Test
    void equalsReturnsFalseForDifferentFields() {
        TableField<Rec, Integer> firstId =
                field("FIRST_ID", Integer.class);
        TableField<Rec, Integer> secondId =
                field("SECOND_ID", Integer.class);

        EntityKey<Rec> first = EntityKey.of(firstId, 10);
        EntityKey<Rec> second = EntityKey.of(secondId, 10);

        assertNotEquals(first, second);
    }

    @Test
    void equalsIgnoresMutableConstructorArgument() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> first =
                new EntityKey<>(new HashMap<>(Map.of(id, 10)), false);

        EntityKey<Rec> second =
                new EntityKey<>(new HashMap<>(Map.of(id, 10)), true);

        assertEquals(first, second);
    }

    @Test
    void toStringContainsFieldNamesAndValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .set(name, "Alice")
                .build();

        String rendered = key.toString();

        assertTrue(rendered.startsWith("{"));
        assertTrue(rendered.endsWith("}"));
        assertTrue(rendered.contains("ID"));
        assertTrue(rendered.contains("10"));
        assertTrue(rendered.contains("NAME"));
        assertTrue(rendered.contains("Alice"));
    }

    @Test
    void toStringContainsNullValues() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        String rendered = key.toString();

        assertTrue(rendered.contains("NAME"));
        assertTrue(rendered.contains("null"));
    }

    @Test
    void toFolderNameReturnsNonEmptyStringForSingleValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        String folderName = key.toFolderName();

        assertNotNull(folderName);
        assertFalse(folderName.isBlank());
    }

    @Test
    void toFolderNameReturnsNonEmptyStringForCompositeValue() {
        TableField<Rec, Integer> firstId =
                field("FIRST_ID", Integer.class);
        TableField<Rec, Integer> secondId =
                field("SECOND_ID", Integer.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(firstId, 1)
                .set(secondId, 2)
                .build();

        String folderName = key.toFolderName();

        assertNotNull(folderName);
        assertFalse(folderName.isBlank());
    }

    @Test
    void immutableReturnsSameBuilderForChaining() {
        EntityKey.Builder<Rec> builder = EntityKey.builder();

        assertSame(builder, builder.immutable());
    }

    @Test
    void setReturnsSameBuilderForChaining() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        EntityKey.Builder<Rec> builder = EntityKey.builder();

        assertSame(builder, builder.set(id, 10));
    }

    @Test
    void setAllReturnsSameBuilderForChaining() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        EntityKey.Builder<Rec> builder = EntityKey.builder();

        assertSame(builder, builder.setAll(Map.of(id, 10)));
    }

    @Test
    void unsafeSetReturnsSameBuilderForChaining() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        EntityKey.Builder<Rec> builder = EntityKey.builder();

        assertSame(builder, builder.unsafeSet(id, 10));
    }
}