package io.github.chechelpo.frplm.core.entities.fields.coercers;

import io.github.chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class ByteArrayCoercer implements Coercer<byte[]> {
    public static final ByteArrayCoercer instance = new ByteArrayCoercer();

    private ByteArrayCoercer() {}

    @Override
    public @NotNull Either<CoerceError, byte[]> coerce(@Nullable Object value) {
        if (value == null) {
            return Either.left(new CoerceError(
                    "Cannot coerce null to byte[]"
            ));
        }

        if (value instanceof byte[] bytes) {
            return Either.right(bytes.clone());
        }

        if (value instanceof Byte[] boxedBytes) {
            byte[] result = new byte[boxedBytes.length];

            for (int index = 0; index < boxedBytes.length; index++) {
                Byte element = boxedBytes[index];

                if (element == null) {
                    return Either.left(new CoerceError(
                            "Cannot coerce Byte[] containing null at index "
                                    + index
                                    + " to byte[]"
                    ));
                }

                result[index] = element;
            }

            return Either.right(result);
        }

        if (value instanceof ByteBuffer buffer) {
            ByteBuffer copy = buffer.asReadOnlyBuffer();
            byte[] result = new byte[copy.remaining()];
            copy.get(result);

            return Either.right(result);
        }

        if (value instanceof String string) {
            return coerceString(string);
        }

        return Either.left(new CoerceError(
                "Cannot coerce value of type "
                        + value.getClass().getTypeName()
                        + " to byte[]"
        ));
    }

    private static @NotNull Either<CoerceError, byte[]> coerceString(
            @NotNull String value
    ) {
        try {
            return Either.right(Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException ignored) {
            /*
             * This fallback treats a non-Base64 string as UTF-8 text.
             *
             * Remove this fallback and return Either.left(...) instead if all
             * string inputs are required to be valid Base64.
             */
            return Either.right(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}