package io.github.chechelpo.frplm.utils.format;

public sealed interface Either<L, R>
        permits Either.Left, Either.Right {

    record Left<L, R>(L value) implements Either<L, R> {}

    record Right<L, R>(R value) implements Either<L, R> {}

    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    default boolean isLeft() {
        return this instanceof Left<L, R>;
    }

    default boolean isRight() {
        return this instanceof Right<L, R>;
    }

    default L leftOrThrow() {
        if (this instanceof Left<L, R> left) {
            return left.value();
        }

        throw new IllegalStateException("Either is Right, not Left");
    }

    default R rightOrThrow() {
        if (this instanceof Right<L, R> right) {
            return right.value();
        }

        throw new IllegalStateException("Either is Left, not Right");
    }

    default <T> T fold(
            java.util.function.Function<? super L, ? extends T> onLeft,
            java.util.function.Function<? super R, ? extends T> onRight
    ) {
        if (this instanceof Left<L, R> left) {
            return onLeft.apply(left.value());
        }

        if (this instanceof Right<L, R> right) {
            return onRight.apply(right.value());
        }

        throw new IllegalStateException("Unknown Either implementation");
    }
}