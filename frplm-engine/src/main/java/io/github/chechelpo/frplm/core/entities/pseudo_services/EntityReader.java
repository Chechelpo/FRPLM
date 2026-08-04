package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.extensions.api.utils.FindResult;
import io.github.chechelpo.frplm.utils.ValidationResult;
import org.jetbrains.annotations.Contract;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.Condition;

public interface EntityReader<R extends TableRecord<R>> {
    /**
     * Validates fields present in a key, ignoring assigned values.
     * @return an error message if the key is invalid
     */
    ValidationResult validateKeyStructure(EntityKey<R> key);

    sealed interface RecordFindResult<R extends TableRecord<R>> extends FindResult<
            R,
            RecordFindResult.NotFound<R>,
            RecordFindResult.Found<R>>
    {
        EntityKey<R> target();

        @Contract("_ -> new")
        static <Rec extends TableRecord<Rec>> RecordFindResult.@NonNull NotFound<Rec> notFound(EntityKey<Rec> target){
            return new RecordFindResult.NotFound<>(target);
        }
        @Contract("_, _ -> new")
        static <Rec extends TableRecord<Rec>> RecordFindResult.@NonNull Found<Rec> found(EntityKey<Rec> target, Rec result){
            return new RecordFindResult.Found<>(target, result);
        }



        default Optional<R> found() {
            return switch (this) {
                case RecordFindResult.Found<R> present -> Optional.of(present.result);
                case RecordFindResult.NotFound<R> ignored -> Optional.empty();
            };
        }

        // ---- Throwing (parity with OneMatchingResult.ifEmptyThrow) ---
        @Override
        default R orElseThrow(){
            if (this instanceof RecordFindResult.NotFound<R> error) throw new EntityNotFound(error, Severity.USER);
            return ((Found<R>) this).result;
        }
        default R orElseThrow(Severity severity){
            if (this instanceof RecordFindResult.NotFound<R> error) throw new EntityNotFound(error, severity);

            return ((Found<R>) this).result;
        }
        @Override
        default R orElseThrow(String message){
            if (this instanceof RecordFindResult.NotFound<R> error) throw new EntityNotFound(message + "\n" + error, Severity.USER);

            return ((Found<R>) this).result;
        }
        default R orElseThrow(String reason, Severity severity){
            if (this instanceof RecordFindResult.NotFound<R> error) throw new EntityNotFound(reason + "\n" + error, severity);

            return ((Found<R>) this).result;
        }

        record NotFound<R extends TableRecord<R>>(EntityKey<R> target) implements RecordFindResult<R>, FindResult.NotFound<
                        R,
                        RecordFindResult.NotFound<R>,
                        RecordFindResult.Found<R>
                >
        {
            public NotFound {
                Objects.requireNonNull(target);
            }

            // ---- Debug-friendly accessors ----

            /**
             * jOOQ table name backing the entity, e.g. {@code CUSTOMER}.
             * Assumes the key has at least one field (same assumption as
             * {@link EntityKey#toString()}).
             */
            public String entityName() {
                return Objects.requireNonNull(firstField().getTable()).getName();
            }

            /**
             * Simple name of the jOOQ record class, e.g. {@code CustomerRecord}.
             */
            public String recordTypeName() {
                return Objects.requireNonNull(firstField().getTable()).getRecordType().getSimpleName();
            }

            public int keyFieldCount() {
                return target.assignments().size();
            }


            /**
             * The equality/pk condition jOOQ would have used for the lookup.
             */
            public Condition keyCondition() {
                return target.getPkCondition();
            }

            /**
             * Rendered SQL for {@link #keyCondition()}, useful for reproducing
             * the miss against a database.
             */
            public String keyConditionSql() {
                return keyCondition().toString();
            }

            /**
             * One-liner debug summary suitable for an {@link EntityNotFound}
             * message or a {@code log.debug(...)} line.
             */
            public String toDebugString() {
                return "No %s entity found; \nkey=%s"
                        .formatted(
                                target == null || target.isEmpty() ? "" : recordTypeName(),
                                target
                        );
            }

            private TableField<R, ?> firstField() {
                return target.assignments().keySet().iterator().next();
            }

            @Override
            public @NonNull String toString() {
                return toDebugString();
            }
        }

        record Found<R extends TableRecord<R>>(EntityKey<R> target, R result) implements RecordFindResult<R>,
                FindResult.Found<R, RecordFindResult.NotFound<R>, RecordFindResult.Found<R>>
        {
            @Override
            public R value() {
                return result;
            }
        }
    }


    RecordFindResult<R> find(EntityKey<R> target);

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
