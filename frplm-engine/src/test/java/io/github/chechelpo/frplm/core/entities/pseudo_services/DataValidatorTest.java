package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.Constraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.chechelpo.frplm.core.entities.pseudo_services.DataValidator.VALIDATION_ERROR.INCOMPLETE_ASSIGNMENTS;
import static io.github.chechelpo.frplm.core.entities.pseudo_services.DataValidator.VALIDATION_ERROR.INVALID_VALUE;
import static io.github.chechelpo.frplm.core.entities.pseudo_services.DataValidator.VALIDATION_ERROR.NON_KEY_FIELD;
import static io.github.chechelpo.frplm.core.entities.pseudo_services.DataValidator.VALIDATION_ERROR.UNKNOWN_FIELD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataValidatorTest {

    private interface Rec extends TableRecord<Rec> {}

    private record TestField<R extends TableRecord<R>>(
            TableField<R, ?> field,
            FieldInfo<?> info
    ) {}

    @SuppressWarnings("unchecked")
    private static @NotNull TableField<Rec, Object> field(String name) {
        TableField<Rec, Object> field = mock(TableField.class);
        when(field.toString()).thenReturn(name);
        return field;
    }

    @SafeVarargs
    private static <R extends TableRecord<R>> Map<TableField<R, ?>, Constraint<?, ?>> constraintsOf(
            TestField<R>... fields
    ) {
        Map<TableField<R, ?>, Constraint<?, ?>> constraints = new LinkedHashMap<>();

        for (TestField<R> field : fields) {
            constraints.put(field.field(), field.info().constraints);
        }

        return constraints;
    }

    @SafeVarargs
    private static <R extends TableRecord<R>> Set<TableField<R, ?>> requiredFieldsOf(
            TestField<R>... fields
    ) {
        return Stream.of(fields)
                .filter(field -> field.info().require)
                .map(TestField::field)
                .collect(Collectors.toSet());
    }

    private static TestField<Rec> requiredIntegerField(String name, long min, long max) {
        TableField<Rec, Object> tableField = field(name);

        FieldInfo<?> info = FieldInfo.numberField(FieldType.INTEGER)
                .setConstraints(
                        NumberConstraint.builder(FieldType.INTEGER)
                                .setMin(min)
                                .setMax(max)
                )
                .require()
                .build();

        return new TestField<>(tableField, info);
    }

    private static TestField<Rec> optionalStringField(String name, int minLength, int maxLength) {
        TableField<Rec, Object> tableField = field(name);

        FieldInfo<?> info = FieldInfo.stringField()
                .setConstraints(
                        StringConstraint.builder()
                                .setMinLength(minLength)
                                .setMaxLength(maxLength)
                )
                .build();

        return new TestField<>(tableField, info);
    }

    private static TestField<Rec> requiredStringField(String name, int minLength, int maxLength) {
        TableField<Rec, Object> tableField = field(name);

        FieldInfo<?> info = FieldInfo.stringField()
                .setConstraints(
                        StringConstraint.builder()
                                .setMinLength(minLength)
                                .setMaxLength(maxLength)
                )
                .require()
                .build();

        return new TestField<>(tableField, info);
    }

    @Test
    void returnsUnknownFieldWhenAssignmentFieldHasNoConstraint() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TableField<Rec, Object> unknown = field("UNKNOWN");

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id),
                        requiredFieldsOf(id),
                        Map.of(unknown, 123),
                        false,
                        false
                );

        assertTrue(result.isPresent());

        DataValidator.FieldValidationError<Rec> error = result.get();

        assertEquals(UNKNOWN_FIELD, error.type());
        assertSame(unknown, error.fieldName());
        assertTrue(error.value().isEmpty());
        assertTrue(error.extraMessage().isEmpty());
    }

    @Test
    void returnsNonKeyFieldWhenOnlyRequiredFieldsAreAllowedAndAssignmentContainsKnownOptionalField() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TestField<Rec> name = optionalStringField("NAME", 2, 20);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id, name),
                        requiredFieldsOf(id, name),
                        Map.of(name.field(), "Alice"),
                        false,
                        true
                );

        assertTrue(result.isPresent());

        DataValidator.FieldValidationError<Rec> error = result.get();

        assertEquals(NON_KEY_FIELD, error.type());
        assertSame(name.field(), error.fieldName());
        assertEquals(Optional.of("Alice"), error.value());
        assertTrue(error.extraMessage().isEmpty());
    }

    @Test
    void acceptsKnownOptionalFieldWhenOnlyRequiredFieldsAreNotRequired() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TestField<Rec> name = optionalStringField("NAME", 2, 20);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id, name),
                        requiredFieldsOf(id, name),
                        Map.of(name.field(), "Alice"),
                        false,
                        false
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsInvalidValueWhenStringConstraintFails() {
        TestField<Rec> name = requiredStringField("NAME", 2, 5);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(name),
                        requiredFieldsOf(name),
                        Map.of(name.field(), "A"),
                        true,
                        true
                );

        assertTrue(result.isPresent());

        DataValidator.FieldValidationError<Rec> error = result.get();

        assertEquals(INVALID_VALUE, error.type());
        assertSame(name.field(), error.fieldName());
        assertEquals(Optional.of("A"), error.value());
        assertTrue(error.extraMessage().isPresent());
        assertTrue(error.extraMessage().get().contains("between 2 and 5"));
    }

    @Test
    void returnsInvalidValueWhenNumberConstraintFails() {
        TestField<Rec> age = requiredIntegerField("AGE", 1, 120);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(age),
                        requiredFieldsOf(age),
                        Map.of(age.field(), 0),
                        true,
                        true
                );

        assertTrue(result.isPresent());

        DataValidator.FieldValidationError<Rec> error = result.get();

        assertEquals(INVALID_VALUE, error.type());
        assertSame(age.field(), error.fieldName());
        assertEquals(Optional.of(0), error.value());
        assertTrue(error.extraMessage().isPresent());
        assertTrue(error.extraMessage().get().contains("between 1 and 120"));
    }

    @Test
    void returnsEmptyWhenAllRequiredFieldsArePresentAndConstraintsPass() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TestField<Rec> name = requiredStringField("NAME", 2, 20);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id, name),
                        requiredFieldsOf(id, name),
                        Map.of(
                                id.field(), 1,
                                name.field(), "Alice"
                        ),
                        true,
                        true
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsIncompleteAssignmentsWhenRequiredFieldIsMissing() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TestField<Rec> name = requiredStringField("NAME", 2, 20);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id, name),
                        requiredFieldsOf(id, name),
                        Map.of(id.field(), 1),
                        true,
                        true
                );

        assertTrue(result.isPresent());

        DataValidator.FieldValidationError<Rec> error = result.get();

        assertEquals(INCOMPLETE_ASSIGNMENTS, error.type());
        assertNull(error.fieldName());
        assertTrue(error.value().isEmpty());
        assertTrue(error.extraMessage().isPresent());
    }

    @Test
    void returnsEmptyWhenMissingRequiredFieldsAreAllowed() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TestField<Rec> name = requiredStringField("NAME", 2, 20);

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id, name),
                        requiredFieldsOf(id, name),
                        Map.of(id.field(), 1),
                        false,
                        true
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void unknownFieldTakesPrecedenceOverNonKeyField() {
        TestField<Rec> id = requiredIntegerField("ID", 1, 100);
        TableField<Rec, Object> unknown = field("UNKNOWN");

        Optional<DataValidator.FieldValidationError<Rec>> result =
                DataValidator.getValidationError(
                        constraintsOf(id),
                        requiredFieldsOf(id),
                        Map.of(unknown, "x"),
                        false,
                        true
                );

        assertTrue(result.isPresent());
        assertEquals(UNKNOWN_FIELD, result.get().type());
        assertSame(unknown, result.get().fieldName());
    }

    @Test
    void toStringContainsErrorTypeFieldValueAndExtraMessage() {
        TableField<Rec, Object> age = field("AGE");

        DataValidator.FieldValidationError<Rec> error =
                new DataValidator.FieldValidationError<>(
                        INVALID_VALUE,
                        age,
                        Optional.of(-1),
                        Optional.of("AGE must be positive")
                );

        String rendered = error.getMessage();

        assertTrue(rendered.contains("INVALID_VALUE"));
        assertTrue(rendered.contains("AGE"));
        assertTrue(rendered.contains("-1"));
        assertTrue(rendered.contains("AGE must be positive"));
    }

    @Test
    void toStringUsesFallbacksWhenValueAndMessageAreAbsent() {
        TableField<Rec, Object> id = field("ID");

        DataValidator.FieldValidationError<Rec> error =
                new DataValidator.FieldValidationError<>(
                        UNKNOWN_FIELD,
                        id,
                        Optional.empty(),
                        Optional.empty()
                );

        String rendered = error.getMessage();

        assertTrue(rendered.contains("UNKNOWN_FIELD"));
        assertTrue(rendered.contains("unimportant"));
        assertTrue(rendered.contains("<no extra section>"));
    }

    @Test
    void fieldValidationErrorRejectsNullType() {
        TableField<Rec, Object> id = field("ID");

        assertThrows(NullPointerException.class, () ->
                new DataValidator.FieldValidationError<>(
                        null,
                        id,
                        Optional.empty(),
                        Optional.empty()
                )
        );
    }

    @Test
    void fieldValidationErrorRejectsNullValueOptional() {
        TableField<Rec, Object> id = field("ID");

        assertThrows(NullPointerException.class, () ->
                new DataValidator.FieldValidationError<>(
                        UNKNOWN_FIELD,
                        id,
                        null,
                        Optional.empty()
                )
        );
    }

    @Test
    void fieldValidationErrorRejectsNullExtraMessageOptional() {
        TableField<Rec, Object> id = field("ID");

        assertThrows(NullPointerException.class, () ->
                new DataValidator.FieldValidationError<>(
                        UNKNOWN_FIELD,
                        id,
                        Optional.empty(),
                        null
                )
        );
    }
}