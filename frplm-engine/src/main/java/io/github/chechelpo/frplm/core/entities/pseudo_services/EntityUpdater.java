package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface EntityUpdater<R extends TableRecord<R>> {

    UpdateResult<R> update(
            EntityKey<R> key,
            EntityDataPayload<R> update
    );

    <T extends Number> IncrementResult<R, T> incrementAndGet(
            TableField<R, T> field,
            EntityKey<R> key
    );

    sealed interface UpdateResult<R extends TableRecord<R>>
            permits UpdateResult.Success,
                    UpdateResult.NoSuchEntity,
                    UpdateResult.Failure {

        <V> V fold(
                Function<? super Success<R>, ? extends V> onSuccess,
                Function<? super NoSuchEntity<R>, ? extends V> onMissing,
                Function<? super Failure<R>, ? extends V> onFailure
        );

        default boolean success() {
            return fold(
                    ignored -> true,
                    ignored -> false,
                    ignored -> false
            );
        }

        default UpdateResult<R> ifSuccess(
                Consumer<? super Success<R>> action
        ) {
            Objects.requireNonNull(action, "action");

            return fold(
                    success -> {
                        action.accept(success);
                        return this;
                    },
                    ignored -> this,
                    ignored -> this
            );
        }

        default UpdateResult<R> ifNoSuchEntity(
                Consumer<? super NoSuchEntity<R>> action
        ) {
            Objects.requireNonNull(action, "action");

            return fold(
                    ignored -> this,
                    missing -> {
                        action.accept(missing);
                        return this;
                    },
                    ignored -> this
            );
        }

        default UpdateResult<R> ifFailure(
                Consumer<? super Failure<R>> action
        ) {
            Objects.requireNonNull(action, "action");

            return fold(
                    ignored -> this,
                    ignored -> this,
                    failure -> {
                        action.accept(failure);
                        return this;
                    }
            );
        }

        record Success<R extends TableRecord<R>>(
                EntityKey<R> key
        ) implements UpdateResult<R> {

            public Success {
                Objects.requireNonNull(key, "key");
            }

            @Override
            public <V> V fold(
                    Function<? super Success<R>, ? extends V> onSuccess,
                    Function<? super NoSuchEntity<R>, ? extends V> onMissing,
                    Function<? super Failure<R>, ? extends V> onFailure
            ) {
                Objects.requireNonNull(onSuccess, "onSuccess");
                Objects.requireNonNull(onMissing, "onMissing");
                Objects.requireNonNull(onFailure, "onFailure");

                return onSuccess.apply(this);
            }
        }

        record NoSuchEntity<R extends TableRecord<R>>(
                EntityKey<R> key
        ) implements UpdateResult<R> {

            public NoSuchEntity {
                Objects.requireNonNull(key, "key");
            }

            @Override
            public <V> V fold(
                    Function<? super Success<R>, ? extends V> onSuccess,
                    Function<? super NoSuchEntity<R>, ? extends V> onMissing,
                    Function<? super Failure<R>, ? extends V> onFailure
            ) {
                Objects.requireNonNull(onSuccess, "onSuccess");
                Objects.requireNonNull(onMissing, "onMissing");
                Objects.requireNonNull(onFailure, "onFailure");

                return onMissing.apply(this);
            }
        }

        record Failure<R extends TableRecord<R>>(
                EntityKey<R> key,
                String message,
                Exception exception
        ) implements UpdateResult<R> {

            public Failure {
                Objects.requireNonNull(key, "key");
                Objects.requireNonNull(message, "message");
                Objects.requireNonNull(exception, "exception");
            }

            @Override
            public <V> V fold(
                    Function<? super Success<R>, ? extends V> onSuccess,
                    Function<? super NoSuchEntity<R>, ? extends V> onMissing,
                    Function<? super Failure<R>, ? extends V> onFailure
            ) {
                Objects.requireNonNull(onSuccess, "onSuccess");
                Objects.requireNonNull(onMissing, "onMissing");
                Objects.requireNonNull(onFailure, "onFailure");

                return onFailure.apply(this);
            }
        }
    }

    sealed interface IncrementResult<
            R extends TableRecord<R>,
            T extends Number
            >
            permits IncrementResult.Success,
                    IncrementResult.NoSuchEntity,
                    IncrementResult.Failure {

        <V> V fold(
                Function<? super Success<R, T>, ? extends V> onSuccess,
                Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
                Function<? super Failure<R, T>, ? extends V> onFailure
        );

        default boolean success() {
            return fold(
                    ignored -> true,
                    ignored -> false,
                    ignored -> false
            );
        }

        default IncrementResult<R, T> ifSuccess(
                Consumer<? super Success<R, T>> action
        ) {
            Objects.requireNonNull(action, "action");

            return fold(
                    success -> {
                        action.accept(success);
                        return this;
                    },
                    ignored -> this,
                    ignored -> this
            );
        }

        default IncrementResult<R, T> ifNoSuchEntity(
                Consumer<? super NoSuchEntity<R, T>> action
        ) {
            Objects.requireNonNull(action, "action");

            return fold(
                    ignored -> this,
                    missing -> {
                        action.accept(missing);
                        return this;
                    },
                    ignored -> this
            );
        }

        default IncrementResult<R, T> ifFailure(
                Consumer<? super Failure<R, T>> action
        ) {
            Objects.requireNonNull(action, "action");

            return fold(
                    ignored -> this,
                    ignored -> this,
                    failure -> {
                        action.accept(failure);
                        return this;
                    }
            );
        }

        record Success<
                R extends TableRecord<R>,
                T extends Number
                >(
                EntityKey<R> key,
                TableField<R, T> field,
                T value
        ) implements IncrementResult<R, T> {

            public Success {
                Objects.requireNonNull(key, "key");
                Objects.requireNonNull(field, "field");
                Objects.requireNonNull(value, "value");
            }

            @Override
            public <V> V fold(
                    Function<? super Success<R, T>, ? extends V> onSuccess,
                    Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
                    Function<? super Failure<R, T>, ? extends V> onFailure
            ) {
                Objects.requireNonNull(onSuccess, "onSuccess");
                Objects.requireNonNull(onMissing, "onMissing");
                Objects.requireNonNull(onFailure, "onFailure");

                return onSuccess.apply(this);
            }
        }

        record NoSuchEntity<
                R extends TableRecord<R>,
                T extends Number
                >(
                EntityKey<R> key,
                TableField<R, T> field
        ) implements IncrementResult<R, T> {

            public NoSuchEntity {
                Objects.requireNonNull(key, "key");
                Objects.requireNonNull(field, "field");
            }

            @Override
            public <V> V fold(
                    Function<? super Success<R, T>, ? extends V> onSuccess,
                    Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
                    Function<? super Failure<R, T>, ? extends V> onFailure
            ) {
                Objects.requireNonNull(onSuccess, "onSuccess");
                Objects.requireNonNull(onMissing, "onMissing");
                Objects.requireNonNull(onFailure, "onFailure");

                return onMissing.apply(this);
            }
        }

        record Failure<
                R extends TableRecord<R>,
                T extends Number
                >(
                EntityKey<R> key,
                TableField<R, T> field,
                String message,
                Exception exception
        ) implements IncrementResult<R, T> {

            public Failure {
                Objects.requireNonNull(key, "key");
                Objects.requireNonNull(field, "field");
                Objects.requireNonNull(message, "message");
                Objects.requireNonNull(exception, "exception");
            }

            @Override
            public <V> V fold(
                    Function<? super Success<R, T>, ? extends V> onSuccess,
                    Function<? super NoSuchEntity<R, T>, ? extends V> onMissing,
                    Function<? super Failure<R, T>, ? extends V> onFailure
            ) {
                Objects.requireNonNull(onSuccess, "onSuccess");
                Objects.requireNonNull(onMissing, "onMissing");
                Objects.requireNonNull(onFailure, "onFailure");

                return onFailure.apply(this);
            }
        }
    }
}