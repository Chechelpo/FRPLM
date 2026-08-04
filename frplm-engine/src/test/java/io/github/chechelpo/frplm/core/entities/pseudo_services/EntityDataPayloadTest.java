package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.exceptions.runtime.ExpectedField;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityDataPayloadTest {

    private interface Rec extends TableRecord<Rec> {
    }

    @SuppressWarnings("unchecked")
    private static <T> TableField<Rec, T> field(
            String name,
            Class<T> type
    ) {
        TableField<Rec, T> field = mock(TableField.class);
        Name unqualifiedName = mock(Name.class);

        when(field.getName()).thenReturn(name);
        when(field.getType()).thenReturn(type);
        when(field.getUnqualifiedName()).thenReturn(unqualifiedName);
        when(unqualifiedName.toString()).thenReturn(name);
        when(field.toString()).thenReturn(name);

        return field;
    }

    @Test
    void defaultConstructorCreatesEmptyPayload() {
        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertTrue(payload.isEmpty());
        assertTrue(payload.assignments().isEmpty());
    }

    @Test
    void builderCreatesEmptyPayloadWhenNoAssignmentsAreSet() {
        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder().build();

        assertTrue(payload.isEmpty());
        assertTrue(payload.assignments().isEmpty());
    }

    @Test
    void staticOfWithoutArgumentsCreatesEmptyPayload() {
        EntityDataPayload<Rec> payload = EntityDataPayload.of();

        assertTrue(payload.isEmpty());
        assertTrue(payload.assignments().isEmpty());
    }

    @Test
    void ofCreatesPayloadWithSingleAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(id, 10);

        DataPayload.Assignment<Rec, Integer> assignment =
                payload.getAssignment(id);

        assertFalse(payload.isEmpty());
        assertTrue(payload.assigns(id));
        assertTrue(assignment.isAssigned());
        assertFalse(assignment.isUnassigned());
        assertEquals(10, assignment.get());
        assertEquals(10, payload.require(id));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void builderSetCreatesPayloadWithMultipleAssignments() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder()
                        .set(id, 1)
                        .set(name, "Alice")
                        .build();

        assertFalse(payload.isEmpty());
        assertTrue(payload.assigns(id));
        assertTrue(payload.assigns(name));
        assertEquals(1, payload.require(id));
        assertEquals("Alice", payload.require(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void setAddsAssignmentAfterConstruction() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.set(name, "Alice");

        assertFalse(payload.isEmpty());
        assertTrue(payload.assigns(name));
        assertEquals("Alice", payload.require(name));
    }

    @Test
    void setCanOverwriteExistingAssignment() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        payload.set(name, "Bob");

        assertEquals("Bob", payload.require(name));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void unsafeSetValueCanStoreValueWithoutCompileTimeTypeCheck() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.unsafeSetValue(id, "not-an-integer");

        assertTrue(payload.assigns(id));
        assertEquals(
                "not-an-integer",
                payload.assignments().get(id)
        );
    }

    @Test
    void setValuesAcceptsValidRuntimeTypes() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 1);
        values.put(name, "Alice");

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.setValues(values);

        assertEquals(1, payload.require(id));
        assertEquals("Alice", payload.require(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void setValuesAcceptsNullRegardlessOfFieldType() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(description, null);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.setValues(values);

        DataPayload.Assignment<Rec, String> assignment =
                payload.getAssignment(description);

        assertTrue(payload.assigns(description));
        assertTrue(assignment.isAssigned());
        assertFalse(assignment.isUnassigned());
        assertNull(assignment.get());
        assertNull(payload.require(description));
    }

    @Test
    void setValuesRejectsInvalidRuntimeType() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, "not-an-integer");

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payload.setValues(values)
        );

        assertTrue(exception.getMessage().contains("Type mismatch"));
        assertTrue(exception.getMessage().contains("ID"));
        assertTrue(
                exception.getMessage().contains(Integer.class.getName())
        );
        assertTrue(
                exception.getMessage().contains(String.class.getName())
        );
        assertTrue(payload.isEmpty());
    }

    @Test
    void setValuesValidatesAllEntriesBeforeMutatingPayload() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Alice");
        values.put(id, "invalid");

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertThrows(
                IllegalArgumentException.class,
                () -> payload.setValues(values)
        );

        assertTrue(payload.isEmpty());
    }

    @Test
    void fromValuesCreatesPayloadWithValidValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 1);
        values.put(name, "Alice");

        EntityDataPayload<Rec> payload =
                EntityDataPayload.fromValues(values);

        assertEquals(1, payload.require(id));
        assertEquals("Alice", payload.require(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void fromValuesRejectsInvalidRuntimeType() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, "not-an-integer");

        assertThrows(
                IllegalArgumentException.class,
                () -> EntityDataPayload.fromValues(values)
        );
    }

    @Test
    void builderSetValuesAcceptsValidRuntimeTypes() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 1);
        values.put(name, "Alice");

        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder()
                        .setValues(values)
                        .build();

        assertEquals(1, payload.require(id));
        assertEquals("Alice", payload.require(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void builderSetValuesRejectsInvalidRuntimeType() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, "not-an-integer");

        EntityDataPayload.Builder<Rec> builder =
                EntityDataPayload.builder();

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setValues(values)
        );

        assertTrue(builder.isEmpty());
    }

    @Test
    void builderAssignsReflectsBuilderStateBeforeBuild() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload.Builder<Rec> builder =
                EntityDataPayload.builder();

        assertFalse(builder.assignsField(id));
        assertTrue(builder.isEmpty());

        builder.set(id, 1);

        assertTrue(builder.assignsField(id));
        assertFalse(builder.isEmpty());
    }

    @Test
    void builderUnsafeSetAddsUncheckedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder()
                        .unsafeSet(id, "invalid")
                        .build();

        assertEquals("invalid", payload.assignments().get(id));
    }

    @Test
    void builderSetReturnsSameBuilderForChaining() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload.Builder<Rec> builder =
                EntityDataPayload.builder();

        assertSame(builder, builder.set(id, 1));
    }

    @Test
    void builderUnsafeSetReturnsSameBuilderForChaining() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload.Builder<Rec> builder =
                EntityDataPayload.builder();

        assertSame(builder, builder.unsafeSet(id, "invalid"));
    }

    @Test
    void builderSetValuesReturnsSameBuilderForChaining() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload.Builder<Rec> builder =
                EntityDataPayload.builder();

        assertSame(
                builder,
                builder.setValues(Map.of(id, 1))
        );
    }

    @Test
    void buildCopiesBuilderAssignments() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload.Builder<Rec> builder =
                EntityDataPayload.builder();

        builder.set(name, "Alice");

        EntityDataPayload<Rec> payload = builder.build();

        builder.set(name, "Bob");

        assertEquals("Alice", payload.require(name));
        assertEquals("Bob", builder.build().require(name));
    }

    @Test
    void constructorCopiesInputMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> source = new HashMap<>();
        source.put(name, "Alice");

        EntityDataPayload<Rec> payload =
                new EntityDataPayload<>(source);

        source.put(name, "Bob");

        assertEquals("Alice", payload.require(name));
    }

    @Test
    void assignmentsReturnsMutableBackingMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.assignments().put(name, "Alice");

        assertTrue(payload.assigns(name));
        assertEquals("Alice", payload.require(name));
    }

    @Test
    void assignmentsReturnsSameBackingMapOnEveryCall() {
        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertSame(
                payload.assignments(),
                payload.assignments()
        );
    }

    @Test
    void getAssignmentReturnsAssignedWrapperForNonNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        DataPayload.Assignment<Rec, String> assignment =
                payload.getAssignment(name);

        assertTrue(assignment.isAssigned());
        assertFalse(assignment.isUnassigned());
        assertSame(name, assignment.field);
        assertEquals("Alice", assignment.get());
        assertEquals("Alice", assignment.orElse("default"));
    }

    @Test
    void getAssignmentReturnsUnassignedWrapperForMissingField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        DataPayload.Assignment<Rec, String> assignment =
                payload.getAssignment(name);

        assertFalse(assignment.isAssigned());
        assertTrue(assignment.isUnassigned());
        assertSame(name, assignment.field);
        assertEquals("default", assignment.orElse("default"));
    }

    @Test
    void getAssignmentReturnsAssignedWrapperForAssignedNullValue() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(description, null);

        DataPayload.Assignment<Rec, String> assignment =
                payload.getAssignment(description);

        assertTrue(payload.assigns(description));
        assertTrue(assignment.isAssigned());
        assertFalse(assignment.isUnassigned());
        assertNull(assignment.get());
        assertNull(assignment.orElse("default"));
    }

    @Test
    void deprecatedGetValueReturnsPresentOptionalForNonNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        Optional<String> value = payload.getValue(name);

        assertEquals(Optional.of("Alice"), value);
    }

    @Test
    void deprecatedGetValueReturnsEmptyForMissingField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertTrue(payload.getValue(name).isEmpty());
    }

    @Test
    void deprecatedGetValueReturnsEmptyForAssignedNull() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(description, null);

        assertTrue(payload.assigns(description));
        assertTrue(payload.getValue(description).isEmpty());
    }

    @Test
    void requireReturnsAssignedValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        assertEquals("Alice", payload.require(name));
    }

    @Test
    void requireReturnsNullForAssignedNull() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(description, null);

        assertTrue(payload.assigns(description));
        assertNull(payload.require(description));
    }

    @Test
    void requireThrowsExpectedFieldForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        ExpectedField exception = assertThrows(
                ExpectedField.class,
                () -> payload.require(name)
        );

        assertTrue(exception.getMessage().contains("NAME"));
        assertTrue(exception.getMessage().contains("is not assigned"));
    }

    @Test
    void requireNonNullReturnsAssignedNonNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        assertEquals("Alice", payload.requireNonNull(name));
    }

    @Test
    void requireNonNullThrowsForAssignedNullValue() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(description, null);

        assertThrows(
                NullPointerException.class,
                () -> payload.requireNonNull(description)
        );
    }

    @Test
    void assignsDistinguishesAssignedNullFromUnassigned() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);
        TableField<Rec, String> name =
                field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(description, null);

        assertTrue(payload.assigns(description));
        assertFalse(payload.assigns(name));
    }

    @Test
    void consumeAddsAssignedValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.consume(
                DataPayload.Assignment.ofAssigned(id, 10)
        );

        assertTrue(payload.assigns(id));
        assertEquals(10, payload.require(id));
    }

    @Test
    void consumeAddsAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.consume(
                DataPayload.Assignment.ofAssigned(name, null)
        );

        assertTrue(payload.assigns(name));
        assertNull(payload.require(name));
    }

    @Test
    void consumeIgnoresUnassignedAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.consume(
                DataPayload.Assignment.ofUnassigned(id)
        );

        assertTrue(payload.isEmpty());
        assertFalse(payload.assigns(id));
    }

    @Test
    void consumeOverwritesExistingAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(id, 10);

        payload.consume(
                DataPayload.Assignment.ofAssigned(id, 20)
        );

        assertEquals(20, payload.require(id));
    }

    @Test
    void consumeIfAbsentAddsMissingAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        payload.consumeIfAbsent(
                DataPayload.Assignment.ofAssigned(id, 10)
        );

        assertEquals(10, payload.require(id));
    }

    @Test
    void consumeIfAbsentDoesNotOverwriteExistingAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(id, 10);

        payload.consumeIfAbsent(
                DataPayload.Assignment.ofAssigned(id, 20)
        );

        assertEquals(10, payload.require(id));
    }

    @Test
    void isEmptyBecomesFalseAfterAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertTrue(payload.isEmpty());

        payload.set(id, 1);

        assertFalse(payload.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void asEqualityConditionsUsesEqForNonNullValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        DataType<Integer> dataType = mock(DataType.class);
        Condition expectedCondition = mock(Condition.class);

        when(id.getDataType()).thenReturn(dataType);
        when(dataType.convert("10")).thenReturn(10);
        when(id.eq(10)).thenReturn(expectedCondition);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();
        payload.unsafeSetValue(id, "10");

        List<Condition> conditions =
                payload.asEqualityConditions();

        assertEquals(1, conditions.size());
        assertSame(expectedCondition, conditions.getFirst());

        verify(dataType).convert("10");
        verify(id).eq(10);
        verify(id, never()).isNull();
    }

    @Test
    void asEqualityConditionsUsesIsNullForNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);
        Condition expectedCondition = mock(Condition.class);

        when(name.isNull()).thenReturn(expectedCondition);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, null);

        List<Condition> conditions =
                payload.asEqualityConditions();

        assertEquals(1, conditions.size());
        assertSame(expectedCondition, conditions.getFirst());

        verify(name).isNull();
        verify(name, never()).getDataType();
        verify(name, never()).eq(nullable(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void asEqualityConditionsCreatesConditionForEveryAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        DataType<Integer> idType = mock(DataType.class);
        DataType<String> nameType = mock(DataType.class);

        Condition idCondition = mock(Condition.class);
        Condition nameCondition = mock(Condition.class);

        when(id.getDataType()).thenReturn(idType);
        when(name.getDataType()).thenReturn(nameType);

        when(idType.convert(10)).thenReturn(10);
        when(nameType.convert("Alice")).thenReturn("Alice");

        when(id.eq(10)).thenReturn(idCondition);
        when(name.eq("Alice")).thenReturn(nameCondition);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder()
                        .set(id, 10)
                        .set(name, "Alice")
                        .build();

        List<Condition> conditions =
                payload.asEqualityConditions();

        assertEquals(2, conditions.size());
        assertTrue(conditions.contains(idCondition));
        assertTrue(conditions.contains(nameCondition));
    }

    @Test
    void asEqualityConditionReturnsNonNullConditionForEmptyPayload() {
        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        Condition condition = payload.asEqualityCondition();

        assertNotNull(condition);
    }

    @SuppressWarnings("unchecked")
    @Test
    void asEqualityConditionCombinesAllConditions() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        DataType<Integer> dataType = mock(DataType.class);
        Condition fieldCondition = mock(Condition.class);

        when(id.getDataType()).thenReturn(dataType);
        when(dataType.convert(10)).thenReturn(10);
        when(id.eq(10)).thenReturn(fieldCondition);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(id, 10);

        Condition combined = payload.asEqualityCondition();

        assertNotNull(combined);
        verify(id).eq(10);
    }

    @Test
    void toStringContainsAssignments() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        String rendered = payload.toString();

        assertTrue(
                rendered.contains("Update object with assignments")
        );
        assertTrue(rendered.contains("Alice"));
    }

    @Test
    void prettyPrintReturnsCompactRepresentationForEmptyPayload() {
        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertEquals(
                "EntityDataPayload {}",
                payload.prettyPrint()
        );
    }

    @Test
    void prettyPrintFormatsStringWithDoubleQuotes() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        String rendered = payload.prettyPrint();

        assertTrue(rendered.startsWith("EntityDataPayload {\n"));
        assertTrue(rendered.contains("NAME = \"Alice\""));
        assertTrue(rendered.endsWith("}"));
    }

    @Test
    void prettyPrintFormatsCharacterWithSingleQuotes() {
        TableField<Rec, Character> code =
                field("CODE", Character.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(code, 'A');

        String rendered = payload.prettyPrint();

        assertTrue(rendered.contains("CODE = 'A'"));
    }

    @Test
    void prettyPrintFormatsNullValue() {
        TableField<Rec, String> description =
                field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(description, null);

        String rendered = payload.prettyPrint();

        assertTrue(rendered.contains("DESCRIPTION = null"));
    }

    @Test
    void prettyPrintFormatsOtherValuesUsingToString() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(id, 10);

        String rendered = payload.prettyPrint();

        assertTrue(rendered.contains("ID = 10"));
    }

    @Test
    void builderSetCanOverwriteExistingAssignmentBeforeBuild() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder()
                        .set(name, "Alice")
                        .set(name, "Bob")
                        .build();

        assertEquals("Bob", payload.require(name));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void builderSetValuesCanOverwriteExistingAssignmentBeforeBuild() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Bob");

        EntityDataPayload<Rec> payload =
                EntityDataPayload.<Rec>builder()
                        .set(name, "Alice")
                        .setValues(values)
                        .build();

        assertEquals("Bob", payload.require(name));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void setValuesCanOverwriteExistingAssignmentAfterConstruction() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Bob");

        EntityDataPayload<Rec> payload =
                EntityDataPayload.of(name, "Alice");

        payload.setValues(values);

        assertEquals("Bob", payload.require(name));
        assertEquals(1, payload.assignments().size());
    }
}