package chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.TableField;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityDataPayloadTest {

    private interface Rec extends TableRecord<Rec> {}

    @SuppressWarnings("unchecked")
    private static <T> TableField<Rec, T> field(String name, Class<T> type) {
        TableField<Rec, T> field = mock(TableField.class);
        when(field.getName()).thenReturn(name);
        when(field.getType()).thenReturn(type);
        when(field.toString()).thenReturn(name);
        return field;
    }

    @Test
    void defaultConstructorCreatesEmptyPayload() {
        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertTrue(payload.isEmpty());
        assertTrue(payload.assignments().isEmpty());
        assertTrue(payload.values().isEmpty());
    }

    @Test
    void builderCreatesEmptyPayloadWhenNoAssignmentsAreSet() {
        EntityDataPayload<Rec> payload = EntityDataPayload.<Rec>builder().build();

        assertTrue(payload.isEmpty());
        assertTrue(payload.assignments().isEmpty());
    }

    @Test
    void ofCreatesPayloadWithSingleAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(id, 10);

        assertFalse(payload.isEmpty());
        assertTrue(payload.assignsField(id));
        assertEquals(10, payload.requireValue(id));
        assertEquals(Optional.of(10), payload.getValue(id));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void builderSetCreatesPayloadWithMultipleAssignments() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.<Rec>builder()
                .set(id, 1)
                .set(name, "Alice")
                .build();

        assertFalse(payload.isEmpty());
        assertTrue(payload.assignsField(id));
        assertTrue(payload.assignsField(name));
        assertEquals(1, payload.requireValue(id));
        assertEquals("Alice", payload.requireValue(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void setAddsAssignmentAfterConstruction() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();
        payload.set(name, "Alice");

        assertFalse(payload.isEmpty());
        assertTrue(payload.assignsField(name));
        assertEquals("Alice", payload.requireValue(name));
    }

    @Test
    void setCanOverwriteExistingAssignment() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(name, "Alice");
        payload.set(name, "Bob");

        assertEquals("Bob", payload.requireValue(name));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void unsafeSetValueCanStoreValueWithoutCompileTimeTypeCheck() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();
        payload.unsafeSetValue(id, "not-an-integer");

        assertTrue(payload.assignsField(id));
        assertEquals("not-an-integer", payload.assignments().get(id));
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

        assertEquals(1, payload.requireValue(id));
        assertEquals("Alice", payload.requireValue(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void setValuesAcceptsNullRegardlessOfFieldType() {
        TableField<Rec, String> description = field("DESCRIPTION", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(description, null);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();
        payload.setValues(values);

        assertTrue(payload.assignsField(description));
        assertNull(payload.requireValue(description));
        assertTrue(payload.getValue(description).isEmpty());
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
        assertTrue(exception.getMessage().contains(Integer.class.getName()));
        assertTrue(exception.getMessage().contains(String.class.getName()));
        assertTrue(payload.isEmpty());
    }

    @Test
    void fromValuesCreatesPayloadWithValidValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 1);
        values.put(name, "Alice");

        EntityDataPayload<Rec> payload = EntityDataPayload.fromValues(values);

        assertEquals(1, payload.requireValue(id));
        assertEquals("Alice", payload.requireValue(name));
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

        EntityDataPayload<Rec> payload = EntityDataPayload.<Rec>builder()
                .setValues(values)
                .build();

        assertEquals(1, payload.requireValue(id));
        assertEquals("Alice", payload.requireValue(name));
        assertEquals(2, payload.assignments().size());
    }

    @Test
    void builderSetValuesRejectsInvalidRuntimeType() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, "not-an-integer");

        EntityDataPayload.Builder<Rec> builder = EntityDataPayload.builder();

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setValues(values)
        );

        assertTrue(builder.isEmpty());
    }

    @Test
    void builderAssignsFieldReflectsBuilderStateBeforeBuild() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        EntityDataPayload.Builder<Rec> builder = EntityDataPayload.builder();

        assertFalse(builder.assignsField(id));

        builder.set(id, 1);

        assertTrue(builder.assignsField(id));
        assertFalse(builder.isEmpty());
    }

    @Test
    void buildCopiesBuilderAssignments() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload.Builder<Rec> builder = EntityDataPayload.builder();
        builder.set(name, "Alice");

        EntityDataPayload<Rec> payload = builder.build();

        builder.set(name, "Bob");

        assertEquals("Alice", payload.requireValue(name));
        assertEquals("Bob", builder.build().requireValue(name));
    }

    @Test
    void constructorCopiesInputMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> source = new HashMap<>();
        source.put(name, "Alice");

        EntityDataPayload<Rec> payload = new EntityDataPayload<>(source);

        source.put(name, "Bob");

        assertEquals("Alice", payload.requireValue(name));
    }

    @Test
    void assignmentsReturnsMutableBackingMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();
        payload.assignments().put(name, "Alice");

        assertTrue(payload.assignsField(name));
        assertEquals("Alice", payload.requireValue(name));
    }

    @Test
    void valuesReturnsSameBackingMapAsAssignments() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();
        payload.assignments().put(name, "Alice");

        assertSame(payload.assignments(), payload.values());
        assertEquals("Alice", payload.values().get(name));
    }

    @Test
    void getValueReturnsPresentOptionalForAssignedNonNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(name, "Alice");

        Optional<String> value = payload.getValue(name);

        assertTrue(value.isPresent());
        assertEquals("Alice", value.get());
    }

    @Test
    void getValueReturnsEmptyOptionalForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertTrue(payload.getValue(name).isEmpty());
    }

    @Test
    void getValueReturnsEmptyOptionalForAssignedNullValue() {
        TableField<Rec, String> description = field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(description, null);

        assertTrue(payload.assignsField(description));
        assertTrue(payload.getValue(description).isEmpty());
    }

    @Test
    void requireValueReturnsNullForAssignedNullValue() {
        TableField<Rec, String> description = field("DESCRIPTION", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(description, null);

        assertTrue(payload.assignsField(description));
        assertNull(payload.requireValue(description));
    }

    @Test
    void requireValueThrowsForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payload.requireValue(name)
        );

        assertTrue(exception.getMessage().contains("Unknown field or unassigned field"));
        assertTrue(exception.getMessage().contains("NAME"));
    }

    @Test
    void assignsFieldDistinguishesAssignedNullFromUnassigned() {
        TableField<Rec, String> description = field("DESCRIPTION", String.class);
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(description, null);

        assertTrue(payload.assignsField(description));
        assertFalse(payload.assignsField(name));
    }

    @Test
    void isEmptyBecomesFalseAfterAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityDataPayload<Rec> payload = new EntityDataPayload<>();

        assertTrue(payload.isEmpty());

        payload.set(id, 1);

        assertFalse(payload.isEmpty());
    }

    @Test
    void toStringContainsAssignments() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.of(name, "Alice");

        String rendered = payload.toString();

        assertTrue(rendered.contains("Update object with assignments"));
        assertTrue(rendered.contains("Alice"));
    }

    @Test
    void builderSetCanOverwriteExistingAssignmentBeforeBuild() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityDataPayload<Rec> payload = EntityDataPayload.<Rec>builder()
                .set(name, "Alice")
                .set(name, "Bob")
                .build();

        assertEquals("Bob", payload.requireValue(name));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void builderSetValuesCanOverwriteExistingAssignmentBeforeBuild() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Bob");

        EntityDataPayload<Rec> payload = EntityDataPayload.<Rec>builder()
                .set(name, "Alice")
                .setValues(values)
                .build();

        assertEquals("Bob", payload.requireValue(name));
        assertEquals(1, payload.assignments().size());
    }

    @Test
    void setValuesCanOverwriteExistingAssignmentAfterConstruction() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Bob");

        EntityDataPayload<Rec> payload = EntityDataPayload.of(name, "Alice");
        payload.setValues(values);

        assertEquals("Bob", payload.requireValue(name));
        assertEquals(1, payload.assignments().size());
    }
}