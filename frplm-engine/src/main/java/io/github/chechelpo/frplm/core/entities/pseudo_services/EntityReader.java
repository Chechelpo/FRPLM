package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import org.jetbrains.annotations.Contract;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Sinks;

import javax.swing.text.html.Option;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jooq.Condition;

public interface EntityReader<R extends TableRecord<R>> {
    sealed interface FindResult<R extends TableRecord<R>> {
        EntityKey<R> target();

        @Contract("_ -> new")
        static <Rec extends TableRecord<Rec>> FindResult.@NonNull NotFound<Rec> notFound(EntityKey<Rec> target){
            return new FindResult.NotFound<>(target);
        }
        @Contract("_, _ -> new")
        static <Rec extends TableRecord<Rec>> FindResult.@NonNull Found<Rec> found(EntityKey<Rec> target, Rec result){
            return new FindResult.Found<>(target, result);
        }

        // ---- Predicates ----

        default boolean isFound() {
            return this instanceof FindResult.Found<R>;
        }

        default boolean isNotFound() {
            return this instanceof FindResult.NotFound<R>;
        }

        // ---- Callback chaining (mirrors OneMatchingResult.if*) ----

        default FindResult<R> ifFound(Consumer<? super R> consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (this instanceof FindResult.Found<R> found) {
                consumer.accept(found.result);
            }
            return this;
        }

        default FindResult<R> ifNotFound(Consumer<? super EntityKey<R>> consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (this instanceof FindResult.NotFound<R> notFound) {
                consumer.accept(notFound.target);
            }
            return this;
        }

        // ---- Defaults / unwrapping ----

        default R orElse(R defaultValue) {
            return isFound() ? ((Found<R>) this).result : defaultValue;
        }

        default R orElseGet(Supplier<? extends R> supplier) {
            Objects.requireNonNull(supplier, "supplier");
            return isFound() ? ((Found<R>) this).result : supplier.get();
        }
        default R get(){
            if (this instanceof FindResult.NotFound<R>)
                throw new IllegalArgumentException("Called for get when this is not found");

            return ((Found<R>) this).result;
        }

        /**
         * Maps the found record to a value of type {@code T}, mirroring
         * {@link Optional#map(Function)}. Returns an empty {@link Optional}
         * when this is a {@link NotFound}; otherwise applies the mapper to
         * the record and wraps the (possibly {@code null}) result in
         * {@link Optional#ofNullable(Object)}.
         */
        default <T> Optional<T> map(Function<? super R, ? extends T> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return switch (this) {
                case FindResult.Found<R> found -> Optional.ofNullable(mapper.apply(found.result));
                case FindResult.NotFound<R> ignored -> Optional.empty();
            };
        }

        default <T> Mapped<R, T> mapResult(Function<? super R, ? extends T> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return switch (this) {
                case FindResult.Found<R> found ->
                        new Mapped.Present<>(target(), Optional.ofNullable(mapper.apply(found.result)));
                case FindResult.NotFound<R> notFound ->
                        new Mapped.Absent<>(notFound.target);
            };
        }

        sealed interface Mapped<R extends TableRecord<R>, T> {
            EntityKey<R> target();
            Optional<T> value();
            record Present<R extends TableRecord<R>, T>(EntityKey<R> target, Optional<T> value) implements Mapped<R, T> {}
            record Absent<R extends TableRecord<R>, T>(EntityKey<R> target) implements Mapped<R, T> {
                @Override public Optional<T> value() { return Optional.empty(); }
            }
        }

        default Stream<R> stream() {
            return isFound() ? Stream.of(((Found<R>) this).result) : Stream.empty();
        }

        /**
         * @deprecated prefer {@link #found()} for naming symmetry with
         * {@link OneMatchingResult.Present#value()}.
         */
        @Deprecated
        default Optional<R> asOptional() {
            return found();
        }

        default Optional<R> found() {
            return switch (this) {
                case FindResult.Found<R> present -> Optional.of(present.result);
                case FindResult.NotFound<R> ignored -> Optional.empty();
            };
        }

        // ---- Throwing (parity with OneMatchingResult.ifEmptyThrow) ----

        default <X extends Throwable> FindResult<R> ifNotFoundThrow(
                Function<? super EntityKey<R>, ? extends X> exceptionFactory
        ) throws X {
            Objects.requireNonNull(exceptionFactory, "exceptionFactory");
            if (this instanceof NotFound<R>(EntityKey<R> target)) {
                throw exceptionFactory.apply(target);
            }
            return this;
        }

        default <X extends Throwable> R orElseThrow(
                Function<NotFound<R>, ? extends X> exceptionFactory
        ) throws X {
            if (this instanceof NotFound<R> notFound)
                throw exceptionFactory.apply(notFound);

            return ((Found<R>) this).result;
        }

        default R orElseThrow(){
            if (this instanceof FindResult.NotFound<R> error) throw new EntityNotFound(error, Severity.USER);
            return ((Found<R>) this).result;
        }
        default R orElseThrow(Severity severity){
            if (this instanceof FindResult.NotFound<R> error) throw new EntityNotFound(error, severity);

            return ((Found<R>) this).result;
        }
        default R orElseThrow(String message){
            if (this instanceof FindResult.NotFound<R> error) throw new EntityNotFound(message + "\n" + error, Severity.USER);

            return ((Found<R>) this).result;
        }
        default R orElseThrow(String reason, Severity severity){
            if (this instanceof FindResult.NotFound<R> error) throw new EntityNotFound(reason + "\n" + error, severity);

            return ((Found<R>) this).result;
        }

        record NotFound<R extends TableRecord<R>>(EntityKey<R> target) implements FindResult<R> {

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
                return target.getValues().size();
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
                return "No %s entity found; \nkey=%s; \ncondition=[%s]"
                        .formatted(recordTypeName(), target, keyConditionSql());
            }

            private TableField<R, ?> firstField() {
                return target.getValues().keySet().iterator().next();
            }

            @Override
            public @NonNull String toString() {
                return toDebugString();
            }
        }

        record Found<R extends TableRecord<R>>(EntityKey<R> target, R result) implements FindResult<R> {}
    }
    FindResult<R> find(EntityKey<R> target);

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
