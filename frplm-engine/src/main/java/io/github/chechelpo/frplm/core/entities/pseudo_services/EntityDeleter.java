package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.TableRecord;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface EntityDeleter<R extends TableRecord<R>> {
    sealed interface Result<R extends TableRecord<R>>
            permits Result.Success,
            Result.NoSuchEntity,
            Result.Error {

        record Success<R extends TableRecord<R>>(
                R deletedRecord
        ) implements Result<R> {
            public Success {
                Objects.requireNonNull(
                        deletedRecord,
                        "deletedRecord"
                );
            }
        }

        record NoSuchEntity<R extends TableRecord<R>>(
                EntityKey<R> key
        ) implements Result<R> {
            public NoSuchEntity {
                Objects.requireNonNull(key, "key");
            }
        }

        record Error<R extends TableRecord<R>>(
                EntityKey<R> key,
                String message,
                Exception exception
        ) implements Result<R> {
            public Error {
                Objects.requireNonNull(key, "key");
                Objects.requireNonNull(message, "message");
                Objects.requireNonNull(exception, "exception");
            }
        }

        default boolean isSuccess() {
            return this instanceof Success<?>;
        }
        default boolean isFailure(){
            return !(this instanceof Success<?>);
        }

        default Result<R> ifSuccess(
                Consumer<? super R> action
        ) {
            Objects.requireNonNull(action, "action");

            if (this instanceof Success<R>(R deletedRecord)) {
                action.accept(deletedRecord);
            }

            return this;
        }

        default Result<R> ifNoSuchEntity(
                Consumer<? super EntityKey<R>> action
        ) {
            Objects.requireNonNull(action, "action");

            if (this instanceof NoSuchEntity<R>(EntityKey<R> key)) {
                action.accept(key);
            }

            return this;
        }

        default Result<R> ifError(
                Consumer<? super Error<R>> action
        ) {
            Objects.requireNonNull(action, "action");

            if (this instanceof Error<R> error) {
                action.accept(error);
            }

            return this;
        }

        default <T> T fold(
                Function<? super R, ? extends T> onSuccess,
                Function<? super EntityKey<R>, ? extends T> onMissing,
                Function<? super Error<R>, ? extends T> onError
        ) {
            Objects.requireNonNull(onSuccess, "onSuccess");
            Objects.requireNonNull(onMissing, "onMissing");
            Objects.requireNonNull(onError, "onError");

            return switch (this) {
                case Success<R> success ->
                        onSuccess.apply(success.deletedRecord());

                case NoSuchEntity<R> missing ->
                        onMissing.apply(missing.key());

                case Error<R> error ->
                        onError.apply(error);
            };
        }
    }

    EntityDeleter.Result<R> delete(EntityKey<R> target);
}
