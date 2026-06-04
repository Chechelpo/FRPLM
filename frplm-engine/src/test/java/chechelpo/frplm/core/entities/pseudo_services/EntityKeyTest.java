package chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.Condition;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityKeyTest {

    private interface Rec extends TableRecord<Rec> {}

    @SuppressWarnings("unchecked")
    private static <T> TableField<Rec, T> field(String name, Class<T> type) {
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

        assertTrue(key.getValues().isEmpty());
        assertEquals(0, key.getEqualityConditions().length);
        assertNotNull(key.getPkCondition());
    }

    @Test
    void builderSetAddsSingleValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .build();

        assertEquals(1, key.getValues().size());
        assertEquals(10, key.getValue(id));
        assertEquals(10, (Integer) key.requireValue(id));
    }

    @Test
    void staticBuilderWithFieldAndValueAddsInitialValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.builder(id, 10)
                .build();

        assertEquals(1, key.getValues().size());
        assertEquals(10, key.getValue(id));
    }

    @Test
    void staticOfCreatesKeyWithSingleValue() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.of(id, 10);

        assertEquals(1, key.getValues().size());
        assertEquals(10, key.getValue(id));
        assertEquals(10, (Integer) key.requireValue(id));
    }

    @Test
    void ofValuesCreatesKeyFromMap() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(id, 10);
        values.put(name, "Alice");

        EntityKey<Rec> key = EntityKey.ofValues(values);

        assertEquals(2, key.getValues().size());
        assertEquals(10, key.getValue(id));
        assertEquals("Alice", key.getValue(name));
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

        assertEquals(2, key.getValues().size());
        assertEquals(10, key.getValue(id));
        assertEquals("Alice", key.getValue(name));
    }

    @Test
    void builderUnsafeSetAddsValueWithoutTypeCheck() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .unsafeSet(id, "not-an-integer")
                .build();

        assertEquals("not-an-integer", key.getValues().get(id));
    }

    @Test
    void builderSetCanOverwriteValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, "Alice")
                .set(name, "Bob")
                .build();

        assertEquals(1, key.getValues().size());
        assertEquals("Bob", key.requireValue(name));
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

        assertEquals(1, key.getValues().size());
        assertEquals("Bob", key.requireValue(name));
    }

    @Test
    void getValueReturnsNullForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder().build();

        assertNull(key.getValue(name));
    }

    @Test
    void requireValueThrowsForUnassignedField() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder().build();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> key.requireValue(name)
        );

        assertTrue(exception.getMessage().contains("NAME"));
        assertTrue(exception.getMessage().contains("is not assigned by this key"));
    }

    @Test
    void requireValueThrowsForAssignedNullValue() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(name, null)
                .build();

        assertTrue(key.getValues().containsKey(name));
        assertNull(key.getValue(name));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> key.requireValue(name)
        );

        assertTrue(exception.getMessage().contains("NAME"));
        assertTrue(exception.getMessage().contains("is not assigned by this key"));
    }

    @Test
    void getValuesReturnsMutableBackingMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey<Rec> key = EntityKey.<Rec>builder().build();

        key.getValues().put(name, "Alice");

        assertEquals("Alice", key.getValue(name));
    }

    @Test
    void constructorDoesNotCopyInputMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        Map<TableField<Rec, ?>, Object> values = new HashMap<>();
        values.put(name, "Alice");

        EntityKey<Rec> key = new EntityKey<>(values, false);

        values.put(name, "Bob");

        assertEquals("Bob", key.getValue(name));
    }

    @Test
    void builderBuildDoesNotCopyBuilderMap() {
        TableField<Rec, String> name = field("NAME", String.class);

        EntityKey.Builder<Rec> builder = EntityKey.<Rec>builder()
                .set(name, "Alice");

        EntityKey<Rec> key = builder.build();

        builder.set(name, "Bob");

        assertEquals("Bob", key.getValue(name));
    }

    @Test
    void getEqualityConditionsUsesEqForNonNullValues() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        Condition eqCondition = condition("ID = 10");

        when(id.eq(10)).thenReturn(eqCondition);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .build();

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
        verify(name, never()).eq((String) any());
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
                (conditions[0] == idCondition && conditions[1] == nameCondition)
                        || (conditions[0] == nameCondition && conditions[1] == idCondition)
        );
    }

    @Test
    void getPkConditionReturnsNonNullConditionForEmptyKey() {
        EntityKey<Rec> key = EntityKey.<Rec>builder().build();

        Condition condition = key.getPkCondition();

        assertNotNull(condition);
    }
    
    @Test
    void getPkConditionReturnsNonNullConditionForSingleAssignment() {
        TableField<Rec, Integer> id = field("ID", Integer.class);
        Condition eqCondition = condition("ID = 10");

        when(id.eq(10)).thenReturn(eqCondition);

        EntityKey<Rec> key = EntityKey.<Rec>builder()
                .set(id, 10)
                .build();

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
        TableField<Rec, Integer> firstId = field("FIRST_ID", Integer.class);
        TableField<Rec, Integer> secondId = field("SECOND_ID", Integer.class);

        EntityKey<Rec> first = EntityKey.of(firstId, 10);
        EntityKey<Rec> second = EntityKey.of(secondId, 10);

        assertNotEquals(first, second);
    }

    @Test
    void equalsIgnoresMutableFlagBecauseOnlyValuesAreCompared() {
        TableField<Rec, Integer> id = field("ID", Integer.class);

        EntityKey<Rec> first = new EntityKey<>(Map.of(id, 10), false);
        EntityKey<Rec> second = new EntityKey<>(Map.of(id, 10), true);

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
        TableField<Rec, Integer> firstId = field("FIRST_ID", Integer.class);
        TableField<Rec, Integer> secondId = field("SECOND_ID", Integer.class);

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