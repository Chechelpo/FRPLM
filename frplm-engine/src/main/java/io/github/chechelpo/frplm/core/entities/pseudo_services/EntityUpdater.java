package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface EntityUpdater<R extends TableRecord<R>> {
    EntityKey<R> keyOf(R record);
    default <T> UpdateResult<R> update(
            TableField<R,T> field,
            T value,
            EntityKey<R> key
    ){
        return update(key, EntityDataPayload.of(field, value));
    }
    default UpdateResult<R> update(
            EntityDataPayload<R> payload,
            R record
    ){
        return update(keyOf(record), payload);
    }
    default <T> UpdateResult<R> update(
            TableField<R,T> field,
            T value,
            R record
    ){
        return update(field, value, keyOf(record));
    }

    UpdateResult<R> update(
            EntityKey<R> key,
            EntityDataPayload<R> update
    );
    default UpdateResult<R> updateOrThrow(
            EntityKey<R> key,
            EntityDataPayload<R> update
    ){
        return update(key, update).orElseThrow();
    }

    sealed interface UpdateResult<R extends TableRecord<R>>
            permits UpdateResult.Success,
                    UpdateResult.NoSuchEntity,
                    UpdateResult.Failure
    {
        EntityKey<R> target();
        EntityDataPayload<R> data();

        default boolean success(){
            return this instanceof UpdateResult.Success<R>;
        }
        default boolean notFound(){
            return this instanceof UpdateResult.Failure<R>;
        }
        default boolean failure(){
            return this instanceof UpdateResult.NoSuchEntity<R>;
        }

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Success
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        default UpdateResult.Success<R> orElseThrow(){
            return (UpdateResult.Success<R>) this.ifEntityNotFoundThrow().ifFailureThrow();
        }
        default UpdateResult.Success<R> orElseThrow(Severity severity){
            return (UpdateResult.Success<R>) this.ifEntityNotFoundThrow(severity).ifFailureThrow(severity);
        }
        default UpdateResult.Success<R> orElseThrow(String message){
            return (UpdateResult.Success<R>) this.ifEntityNotFoundThrow(message).ifFailureThrow(message);
        }
        default UpdateResult.Success<R> orElseThrow(String message, Severity severity){
            return (UpdateResult.Success<R>) this.ifEntityNotFoundThrow(message, severity).ifFailureThrow(message, severity);
        }
        default <T> Optional<T> map(Function<? super UpdateResult.Success<R>, T> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return switch (this){
                case UpdateResult.Failure<R> ignored -> Optional.empty();
                case NoSuchEntity<R> ignored -> Optional.empty();
                case Success<R> success -> Optional.ofNullable(mapper.apply(success));
            };
        }

        record Success<R extends TableRecord<R>>(EntityKey<R> target, EntityDataPayload<R> data) implements UpdateResult<R> {
            public Success {
                Objects.requireNonNull(target, "key");
            }
        }
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Not found
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        default <X extends Throwable> UpdateResult<R> ifEntityNotFoundThrow(){
            if (this instanceof UpdateResult.NoSuchEntity<R> e)
                e.findResult.orElseThrow(
                        "Couldn't update with assignments %s: \n%s".formatted(this.data(), e.findResult().toDebugString()),
                        Severity.USER
                );

            return this;
        }
        default <X extends Throwable> UpdateResult<R> ifEntityNotFoundThrow(Severity severity){
            if (this instanceof UpdateResult.NoSuchEntity<R> e)
                e.findResult.orElseThrow(
                        "Couldn't update with assignments %s: ".formatted(this.data()),
                        severity
                );

            return this;
        }
        default <X extends Throwable> UpdateResult<R> ifEntityNotFoundThrow(String message){
            if (this instanceof UpdateResult.NoSuchEntity<R> e)
                e.findResult.orElseThrow(
                        message + "\n Couldn't update with assignments %s: ".formatted(this.data()),
                        Severity.USER
                );

            return this;
        }
        default <X extends Throwable> UpdateResult<R> ifEntityNotFoundThrow(String message, Severity severity){
            if (this instanceof UpdateResult.NoSuchEntity<R> e)
                e.findResult.orElseThrow(
                        message + "\nCouldn't update with assignments %s: ".formatted(this.data()),
                        severity
                );

            return this;
        }
        record NoSuchEntity<R extends TableRecord<R>>(
                EntityReader.RecordFindResult.NotFound<R> findResult, EntityDataPayload<R> data
        ) implements UpdateResult<R> {
            public NoSuchEntity {
                Objects.requireNonNull(findResult, "key");
            }

            @Override
            public EntityKey<R> target() {
                return this.findResult.target();
            }
        }

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Failure
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        default <X extends Throwable> UpdateResult<R> ifFailureThrow(){
            if (this instanceof UpdateResult.Failure<R> f){
                throw new UnexpectedException(
                        f.toDebugString(),
                        Severity.USER
                );
            }

            return this;
        }
        default <X extends Throwable> UpdateResult<R> ifFailureThrow(Severity severity){
            if (this instanceof UpdateResult.Failure<R> f){
                throw new UnexpectedException(
                        f.toDebugString(),
                        Severity.USER
                );
            }

            return this;
        }
        default <X extends Throwable> UpdateResult<R> ifFailureThrow(String message){
            if (this instanceof UpdateResult.Failure<R> f){
                throw new UnexpectedException(
                        message + "\n" + f.toDebugString(),
                        Severity.USER
                );
            }

            return this;
        }
        default <X extends Throwable> UpdateResult<R> ifFailureThrow(String message, Severity severity){
            if (this instanceof UpdateResult.Failure<R> f){
                throw new UnexpectedException(
                        message + "\n" + f.toDebugString(),
                        severity
                );
            }

            return this;
        }


        record Failure<R extends TableRecord<R>>(
                EntityKey<R> target,
                EntityDataPayload<R> data,
                Exception exception
        ) implements UpdateResult<R> {
            public Failure {
                Objects.requireNonNull(target, "key");
                Objects.requireNonNull(data, "message");
                Objects.requireNonNull(exception, "exception");
            }
            String toDebugString(){
                return """
                        Couldn't update target:
                            %s
                        With assignments (new data):
                            %s
                        Trace:
                            %s
                        """.formatted(
                                target,
                        data,
                        Arrays.stream(exception.getStackTrace()).map(StackTraceElement::toString)
                );
            }
        }
    }
//
//    <T extends Number> IncrementResult<R, T> incrementAndGet(
//            TableField<R, T> field,
//            EntityKey<R> key
//    );
//
//    sealed interface IncrementResult<R extends TableRecord<R>, T extends Number>
//            permits IncrementResult.Success,
//                    IncrementResult.NoSuchEntity,
//                    IncrementResult.Failure
//    {
//
//        <V> V fold(
//                Function<? super Success<R, T>, ? extends V> onSuccess,
//                Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
//                Function<? super Failure<R, T>, ? extends V> onFailure
//        );
//
//        default boolean success() {
//            return fold(
//                    ignored -> true,
//                    ignored -> false,
//                    ignored -> false
//            );
//        }
//
//        default IncrementResult<R, T> ifSuccess(
//                Consumer<? super Success<R, T>> action
//        ) {
//            Objects.requireNonNull(action, "action");
//
//            return fold(
//                    success -> {
//                        action.accept(success);
//                        return this;
//                    },
//                    ignored -> this,
//                    ignored -> this
//            );
//        }
//
//        default IncrementResult<R, T> ifNoSuchEntity(
//                Consumer<? super NoSuchEntity<R, T>> action
//        ) {
//            Objects.requireNonNull(action, "action");
//
//            return fold(
//                    ignored -> this,
//                    missing -> {
//                        action.accept(missing);
//                        return this;
//                    },
//                    ignored -> this
//            );
//        }
//
//        default IncrementResult<R, T> ifFailure(
//                Consumer<? super Failure<R, T>> action
//        ) {
//            Objects.requireNonNull(action, "action");
//
//            return fold(
//                    ignored -> this,
//                    ignored -> this,
//                    failure -> {
//                        action.accept(failure);
//                        return this;
//                    }
//            );
//        }
//
//        record Success<
//                R extends TableRecord<R>,
//                T extends Number
//                >(
//                EntityKey<R> key,
//                TableField<R, T> field,
//                T value
//        ) implements IncrementResult<R, T> {
//
//            public Success {
//                Objects.requireNonNull(key, "key");
//                Objects.requireNonNull(field, "field");
//                Objects.requireNonNull(value, "value");
//            }
//
//            @Override
//            public <V> V fold(
//                    Function<? super Success<R, T>, ? extends V> onSuccess,
//                    Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
//                    Function<? super Failure<R, T>, ? extends V> onFailure
//            ) {
//                Objects.requireNonNull(onSuccess, "onSuccess");
//                Objects.requireNonNull(onMissing, "onMissing");
//                Objects.requireNonNull(onFailure, "onFailure");
//
//                return onSuccess.apply(this);
//            }
//        }
//
//        record NoSuchEntity<
//                R extends TableRecord<R>,
//                T extends Number
//                >(
//                EntityKey<R> key,
//                TableField<R, T> field
//        ) implements IncrementResult<R, T> {
//
//            public NoSuchEntity {
//                Objects.requireNonNull(key, "key");
//                Objects.requireNonNull(field, "field");
//            }
//
//            @Override
//            public <V> V fold(
//                    Function<? super Success<R, T>, ? extends V> onSuccess,
//                    Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
//                    Function<? super Failure<R, T>, ? extends V> onFailure
//            ) {
//                Objects.requireNonNull(onSuccess, "onSuccess");
//                Objects.requireNonNull(onMissing, "onMissing");
//                Objects.requireNonNull(onFailure, "onFailure");
//
//                return onMissing.apply(this);
//            }
//        }
//
//        record Failure<
//                R extends TableRecord<R>,
//                T extends Number
//                >(
//                EntityKey<R> key,
//                TableField<R, T> field,
//                String message,
//                Exception exception
//        ) implements IncrementResult<R, T> {
//
//            public Failure {
//                Objects.requireNonNull(key, "key");
//                Objects.requireNonNull(field, "field");
//                Objects.requireNonNull(message, "message");
//                Objects.requireNonNull(exception, "exception");
//            }
//
//            @Override
//            public <V> V fold(
//                    Function<? super Success<R, T>, ? extends V> onSuccess,
//                    Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
//                    Function<? super Failure<R, T>, ? extends V> onFailure
//            ) {
//                Objects.requireNonNull(onSuccess, "onSuccess");
//                Objects.requireNonNull(onMissing, "onMissing");
//                Objects.requireNonNull(onFailure, "onFailure");
//
//                return onFailure.apply(this);
//            }
//        }
//    }
}