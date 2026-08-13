package io.github.chechelpo.frplm.core.entities.fields;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.ExpectedField;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface DataPayload<R extends TableRecord<R>> permits EntityKey, EntityDataPayload {
    Map<TableField<R , ?>, Object> assignments();

    final class Assignment<R extends TableRecord<R>, T> {
        public final TableField<R, T> field;
        private final T value;
        private final boolean assigned;

        @Contract(value = "_, _ -> new", pure = true)
        public static <R extends TableRecord<R>, T> @NonNull Assignment<R, T> ofAssigned(TableField<R, T> field, T value){
            return new Assignment<>(field, value);
        }
        @Contract(value = "_ -> new", pure = true)
        public static <R extends TableRecord<R>, T> @NonNull Assignment<R, T> ofUnassigned(TableField<R, T> field){
            return new Assignment<>(field);
        }

        private Assignment(TableField<R, T> field) {
            assigned = false;
            this.field = field;
            this.value = null;
        }
        private Assignment(TableField<R,T> field, T value){
            assigned = true;
            this.field = field;
            this.value = value;
        }

        public boolean isAssigned(){
            return assigned;
        }
        public boolean isUnassigned() {
            return !assigned;
        }

        public @Nullable T orElse(T defaultValue){
            if (assigned) return this.value;
            return defaultValue;
        }
        public @Nullable T orElseGet(Supplier<T> constructor){
            if (assigned) return this.value;
            return constructor.get();
        }
        public Assignment<R,T> ifUnassignedRun(Runnable runnable){
            if (isUnassigned()) runnable.run();
            return this;
        }
        public <Q> Optional<Q> map(Function<T, Q> mapper){
            if (assigned) return Optional.of(mapper.apply(value));
            return Optional.empty();
        }

        public Assignment<R, T> ifAssigned(Consumer<T> action){
            if (isAssigned())
                action.accept(this.value);

            return this;
        }
        public Assignment<R,T> ifAssignedNotNull(Consumer<T> action) {
            if (isAssigned() && this.value != null)
                action.accept(this.value);

            return this;
        }

        public <E extends Exception> @Nullable T orElseThrow(Function<TableField<R, T>, E> exceptionProvider) throws E {
            if (assigned) return value;
            throw exceptionProvider.apply(field);
        }

        private @NonNull String getMessage() {
            return "Field " + field + " is not assigned";
        }

        public @Nullable T orElseThrow() throws ExpectedField {
            return orElseThrow(getMessage(), Severity.USER);
        }
        public @Nullable T orElseThrow(Severity severity){
            return orElseThrow(getMessage(), severity);
        }
        public @Nullable T orElseThrow(String message){
            return orElseThrow(message, Severity.USER);
        }
        public @Nullable T orElseThrow(String message, Severity severity){
            if (assigned) return value;
            throw new ExpectedField(message, severity);
        }

        public @Nullable T get(){
            if (!assigned) throw new NullPointerException("Field " + field + " has no assignment");
            return value;
        }
    }

    default <T> @NotNull T requireNonNull(TableField<R,T> field){
        return Objects.requireNonNull(require(field));
    }
    default <T> @Nullable T require(TableField<R,T> field){
        return getAssignment(field).orElseThrow();
    }


    <T> Assignment<R, T> getAssignment(TableField<R, T> field);

    default boolean assignsAny(Collection<? extends TableField<R,?>> fields){
        boolean fail = false;

        for (var field : fields) if (!assigns(field)) return fail;

        return !fail;
    }

    default void requireAssignments(Collection<? extends TableField<R, ?>> fields, boolean nonNull){
        for (TableField<R, ?> field : fields)
            requireAssignment(field, nonNull);
    }
    default void requireAssignment(TableField<R, ?> field, boolean nonNull) {
        Assignment<R, ?> assignment = getAssignment(field);

        if (assignment.isUnassigned()
                || (nonNull && assignment.get() == null)) {
            throw new ExpectedField(
                    "Expected field " + field.getName() + " to be assigned",
                    Severity.USER
            );
        }
    }

    boolean assigns(TableField<R, ?> field);
    /** @return a new TableRecord object with the same values as this */
    default R toRecord(Supplier<? extends R> constructor) {
        R record = constructor.get();

        assignments().forEach((field, value) ->
                setFieldValue(record, field, value)
        );

        return record;
    }

    @SuppressWarnings("unchecked")
    private static <R extends TableRecord<R>, T> void setFieldValue(
            R record,
            TableField<R, ?> field,
            Object value
    ) {
        TableField<R, T> typedField = (TableField<R, T>) field;
        record.set(typedField, (T) value);
    }

    default void consumeIfAbsent(@NonNull Assignment<R, ?> assignment){
        if (this.assigns(assignment.field)) return;
        this.consume(assignment);
    }
    void consume(Assignment<R, ?> assignment);

    boolean isEmpty();
}
