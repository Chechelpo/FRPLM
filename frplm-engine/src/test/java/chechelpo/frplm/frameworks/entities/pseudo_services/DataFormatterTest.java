package chechelpo.frplm.frameworks.entities.pseudo_services;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.coercers.Coercer;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.utils.format.Either;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static chechelpo.frplm.frameworks.entities.pseudo_services.DataFormatter.ERROR_TYPE.COERCER_ERROR;
import static chechelpo.frplm.frameworks.entities.pseudo_services.DataFormatter.ERROR_TYPE.UNKNOWN_FIELD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFormatterTest {

    private interface Rec extends TableRecord<Rec> {}

    private record TestField<R extends TableRecord<R>>(
            String externalName,
            TableField<R, ?> field,
            FieldInfo<?> info
    ) {}

    @SuppressWarnings("unchecked")
    private static TableField<Rec, Object> field(String name) {
        TableField<Rec, Object> field = mock(TableField.class);
        when(field.toString()).thenReturn(name);
        return field;
    }

    private static TestField<Rec> stringField(String externalName) {
        return new TestField<>(
                externalName,
                field(externalName.toUpperCase()),
                FieldInfo.stringField().build()
        );
    }

    private static TestField<Rec> integerField(String externalName) {
        return new TestField<>(
                externalName,
                field(externalName.toUpperCase()),
                FieldInfo.numberField(FieldType.INTEGER).build()
        );
    }

    private static TestField<Rec> doubleField(String externalName) {
        return new TestField<>(
                externalName,
                field(externalName.toUpperCase()),
                FieldInfo.floatField(FieldType.DOUBLE).build()
        );
    }

    private static TestField<Rec> booleanField(String externalName) {
        return new TestField<>(
                externalName,
                field(externalName.toUpperCase()),
                FieldInfo.booleanField().build()
        );
    }

    @SafeVarargs
    private static <R extends TableRecord<R>> Map<String, TableField<R, ?>> translatorOf(
            TestField<R>... fields
    ) {
        Map<String, TableField<R, ?>> translator = new LinkedHashMap<>();

        for (TestField<R> field : fields) {
            translator.put(field.externalName(), field.field());
        }

        return translator;
    }

    @SafeVarargs
    private static <R extends TableRecord<R>> Map<TableField<R, ?>, Coercer<?>> coercersOf(
            TestField<R>... fields
    ) {
        Map<TableField<R, ?>, Coercer<?>> coercers = new LinkedHashMap<>();

        for (TestField<R> field : fields) {
            coercers.put(field.field(), field.info().format);
        }

        return coercers;
    }

    @Test
    void returnsRightWithCoercedStringValue() {
        TestField<Rec> name = stringField("name");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("name", "Alice"),
                        translatorOf(name),
                        coercersOf(name)
                );

        assertTrue(result.isRight());

        Map<TableField<Rec, ?>, Object> values = result.rightOrThrow();

        assertEquals(1, values.size());
        assertEquals("Alice", values.get(name.field()));
    }

    @Test
    void returnsRightWithCoercedIntegerValueFromString() {
        TestField<Rec> age = integerField("age");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("age", "42"),
                        translatorOf(age),
                        coercersOf(age)
                );

        assertTrue(result.isRight());

        Map<TableField<Rec, ?>, Object> values = result.rightOrThrow();

        assertEquals(1, values.size());
        assertEquals(42, values.get(age.field()));
    }

    @Test
    void returnsRightWithCoercedDoubleValueFromString() {
        TestField<Rec> price = doubleField("price");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("price", "12.5"),
                        translatorOf(price),
                        coercersOf(price)
                );

        assertTrue(result.isRight());

        Map<TableField<Rec, ?>, Object> values = result.rightOrThrow();

        assertEquals(1, values.size());
        assertEquals(12.5, values.get(price.field()));
    }

    @Test
    void returnsRightWithCoercedBooleanValueFromString() {
        TestField<Rec> active = booleanField("active");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("active", "true"),
                        translatorOf(active),
                        coercersOf(active)
                );

        assertTrue(result.isRight());

        Map<TableField<Rec, ?>, Object> values = result.rightOrThrow();

        assertEquals(1, values.size());
        assertEquals(true, values.get(active.field()));
    }

    @Test
    void returnsRightWithMultipleCoercedValues() {
        TestField<Rec> name = stringField("name");
        TestField<Rec> age = integerField("age");
        TestField<Rec> price = doubleField("price");
        TestField<Rec> active = booleanField("active");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "Alice");
        params.put("age", "42");
        params.put("price", "12.5");
        params.put("active", "true");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        params,
                        translatorOf(name, age, price, active),
                        coercersOf(name, age, price, active)
                );

        assertTrue(result.isRight());

        Map<TableField<Rec, ?>, Object> values = result.rightOrThrow();

        assertEquals("Alice", values.get(name.field()));
        assertEquals(42, values.get(age.field()));
        assertEquals(12.5, values.get(price.field()));
        assertEquals(true, values.get(active.field()));
    }

    @Test
    void returnsRightWithNullValueWhenCoercerAcceptsNull() {
        TestField<Rec> name = stringField("name");

        Map<String, Object> params = new HashMap<>();
        params.put("name", null);

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        params,
                        translatorOf(name),
                        coercersOf(name)
                );

        assertTrue(result.isRight());

        Map<TableField<Rec, ?>, Object> values = result.rightOrThrow();

        assertTrue(values.containsKey(name.field()));
        assertNull(values.get(name.field()));
    }

    @Test
    void returnsUnknownFieldErrorWhenTranslatorDoesNotContainParamName() {
        TestField<Rec> name = stringField("name");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("unknown", "Alice"),
                        translatorOf(name),
                        coercersOf(name)
                );

        assertTrue(result.isLeft());

        DataFormatter.FormatError<Rec> error = result.leftOrThrow();

        assertEquals(UNKNOWN_FIELD, error.type());
        assertEquals("Unknown field: unknown", error.message());
    }

    @Test
    void returnsCoercerErrorWhenIntegerCoercionFails() {
        TestField<Rec> age = integerField("age");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("age", "not-an-integer"),
                        translatorOf(age),
                        coercersOf(age)
                );

        assertTrue(result.isLeft());

        DataFormatter.FormatError<Rec> error = result.leftOrThrow();

        assertEquals(COERCER_ERROR, error.type());
        assertTrue(error.message().contains("not-an-integer"));
    }

    @Test
    void returnsCoercerErrorWhenDoubleCoercionFails() {
        TestField<Rec> price = doubleField("price");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("price", "not-a-double"),
                        translatorOf(price),
                        coercersOf(price)
                );

        assertTrue(result.isLeft());

        DataFormatter.FormatError<Rec> error = result.leftOrThrow();

        assertEquals(COERCER_ERROR, error.type());
        assertTrue(error.message().contains("not-a-double"));
    }

    @Test
    void returnsCoercerErrorWhenStringCoercionFailsForNonStringInput() {
        TestField<Rec> name = stringField("name");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        Map.of("name", 123),
                        translatorOf(name),
                        coercersOf(name)
                );

        assertTrue(result.isLeft());

        DataFormatter.FormatError<Rec> error = result.leftOrThrow();

        assertEquals(COERCER_ERROR, error.type());
        assertTrue(error.message().contains("123"));
    }

    @Test
    void unknownFieldTakesPrecedenceOverCoercion() {
        TestField<Rec> age = integerField("age");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("unknown", "not-an-integer");
        params.put("age", "42");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        params,
                        translatorOf(age),
                        coercersOf(age)
                );

        assertTrue(result.isLeft());

        DataFormatter.FormatError<Rec> error = result.leftOrThrow();

        assertEquals(UNKNOWN_FIELD, error.type());
        assertEquals("Unknown field: unknown", error.message());
    }

    @Test
    void coercerErrorShortCircuitsBeforeLaterValidFields() {
        TestField<Rec> age = integerField("age");
        TestField<Rec> name = stringField("name");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("age", "not-an-integer");
        params.put("name", "Alice");

        Either<DataFormatter.FormatError<Rec>, Map<TableField<Rec, ?>, Object>> result =
                DataFormatter.coerceValues(
                        params,
                        translatorOf(age, name),
                        coercersOf(age, name)
                );

        assertTrue(result.isLeft());

        DataFormatter.FormatError<Rec> error = result.leftOrThrow();

        assertEquals(COERCER_ERROR, error.type());
        assertTrue(error.message().contains("not-an-integer"));
    }
}