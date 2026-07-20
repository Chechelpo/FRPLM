package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.TableRecord;
import reactor.core.publisher.Sinks;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface EntityReader<R extends TableRecord<R>> {
    Optional<R> find(EntityKey<R> target);

    Result<R> getAll();

    @Deprecated
    Result<R> getMatching(EntityKey<R> target);

    Result<R> getMatching(EntityDataPayload<R> target);

    <T> Result<R> getMatching(TableField<R, T> field, T value);

    sealed interface OneMatchingResult<R extends TableRecord<R>>
            permits OneMatchingResult.Empty,
            OneMatchingResult.Present,
            OneMatchingResult.MoreThanOne {

        EntityDataPayload<R> target();

        default boolean isEmpty() {
            return this instanceof Empty<R>;
        }

        default boolean isPresent() {
            return this instanceof Present<R>;
        }

        default boolean isMoreThanOne() {
            return this instanceof MoreThanOne<R>;
        }

        default OneMatchingResult<R> ifEmpty(
                Consumer<? super EntityDataPayload<R>> consumer
        ) {
            Objects.requireNonNull(consumer, "consumer");

            if (this instanceof Empty<R> empty) {
                consumer.accept(empty.target());
            }

            return this;
        }

        default OneMatchingResult<R> ifMoreThanOne(
                Consumer<? super MoreThanOne<R>> consumer
        ) {
            Objects.requireNonNull(consumer, "consumer");

            if (this instanceof MoreThanOne<R> multiple) {
                consumer.accept(multiple);
            }

            return this;
        }

        default OneMatchingResult<R> ifPresent(
                Consumer<? super R> consumer
        ) {
            Objects.requireNonNull(consumer, "consumer");

            if (this instanceof Present<R> present) {
                consumer.accept(present.value());
            }

            return this;
        }

        default <X extends Throwable> OneMatchingResult<R> ifEmptyThrow(
                Function<? super EntityDataPayload<R>, ? extends X> exceptionFactory
        ) throws X {
            Objects.requireNonNull(exceptionFactory, "exceptionFactory");

            if (this instanceof Empty<R>(EntityDataPayload<R> target)) {
                throw exceptionFactory.apply(target);
            }

            return this;
        }
        default OneMatchingResult<R> ifEmptyThrow(){
            if (this instanceof Empty<R> empty)
                throw new EntityNotFound(
                        "Could not find entity with data \n" + target(),
                        Severity.SYSTEM
                );

            return this;
        }

        default <X extends Throwable> OneMatchingResult<R> ifMoreThanOneThrow(
                Function<? super MoreThanOne<R>, ? extends X> exceptionFactory
        ) throws X {
            Objects.requireNonNull(exceptionFactory, "exceptionFactory");

            if (this instanceof MoreThanOne<R> multiple) {
                throw exceptionFactory.apply(multiple);
            }

            return this;
        }
        default OneMatchingResult<R> ifMoreThanOneThrow(){
            if (this instanceof MoreThanOne<R>(EntityDataPayload<R> target, long matchCount))
                throw new UnexpectedException(
                        "Expected one result of query, instead got %s. Query params: \n%s".formatted(matchCount, target),
                        Severity.SYSTEM
                );
            return this;
        }

        /**
         * Resolves this result using the default exceptions for empty and
         * multiple-match states.
         *
         * @return the single matching value
         * @throws EntityNotFound if no records matched
         * @throws UnexpectedException if multiple records matched
         */
        default R resolve(){
            return ((Present<R>) this.ifMoreThanOneThrow().ifEmptyThrow()).value;
        }

        default Optional<R> asOptional() {
            return switch (this) {
                case Present<R> present -> Optional.of(present.value());
                case Empty<R> ignored -> Optional.empty();
                case MoreThanOne<R> ignored -> Optional.empty();
            };
        }

        default <
                E extends Throwable,
                M extends Throwable
                > R orElseThrow(
                Function<? super EntityDataPayload<R>, ? extends E> emptyException,
                Function<? super MoreThanOne<R>, ? extends M> multipleException
        ) throws E, M {
            Objects.requireNonNull(emptyException, "emptyException");
            Objects.requireNonNull(multipleException, "multipleException");

            return switch (this) {
                case Present<R> present -> present.value();
                case Empty<R> empty ->
                        throw emptyException.apply(empty.target());
                case MoreThanOne<R> multiple ->
                        throw multipleException.apply(multiple);
            };
        }

        record Empty<R extends TableRecord<R>>(
                EntityDataPayload<R> target
        ) implements OneMatchingResult<R> {

            public Empty {
                Objects.requireNonNull(target, "target");
            }
        }

        record Present<R extends TableRecord<R>>(
                EntityDataPayload<R> target,
                R value
        ) implements OneMatchingResult<R> {

            public Present {
                Objects.requireNonNull(target, "target");
                Objects.requireNonNull(value, "value");
            }
        }

        record MoreThanOne<R extends TableRecord<R>>(
                EntityDataPayload<R> target,
                long matchCount
        ) implements OneMatchingResult<R> {

            public MoreThanOne {
                Objects.requireNonNull(target, "target");

                if (matchCount < 2) {
                    throw new IllegalArgumentException(
                            "matchCount must be at least 2"
                    );
                }
            }
        }
    }

    OneMatchingResult<R> getOneMatching(EntityDataPayload<R> target);
    <T> OneMatchingResult<R> getOneMatching(TableField<R,T> field, T value);
}
