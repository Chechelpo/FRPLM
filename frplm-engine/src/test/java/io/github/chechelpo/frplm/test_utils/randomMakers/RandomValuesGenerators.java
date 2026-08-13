package io.github.chechelpo.frplm.test_utils.randomMakers;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.fields.constraints.*;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public final class RandomValuesGenerators<
        R extends TableRecord<R>
        > {

    private static final char[] STRING_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                    .toCharArray();

    private static final int MAX_ATTEMPTS = 1_000;

    private final long seed;
    private final FieldValidator<R> validator;

    private final Map<Class<?>, ValueGenerator<?>> customGenerators =
            new HashMap<>();

    public RandomValuesGenerators(long seed, FieldValidator<R> validator) {
        this.seed = seed;
        this.validator = validator;
    }

    public RandomValuesGenerators(String seed, FieldValidator<R> validator) {
        this(seedFrom(seed), validator);
    }

    /**
     * Overrides generation for an entire Java type.
     *
     * Registering int.class also applies to Integer.class.
     */
    public <T> RandomValuesGenerators<R> setGenerator(
            Class<T> type,
            ValueGenerator<T> generator
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(generator);

        customGenerators.put(
                boxedType(type),
                generator
        );

        return this;
    }

    /**
     * Generates using the base seed.
     */
    public <T> T getValueFor(
            TableField<R, T> field
    ) {
        return getValueFor(field, 0L);
    }

    /**
     * Generates using:
     *
     * base seed
     * + field identity
     * + entity seed delta
     *
     * This allows the same fixture to deterministically create
     * many different entities.
     */
    public <T> T getValueFor(
            TableField<R, T> field,
            long seedDelta
    ) {
        Objects.requireNonNull(field);

        FieldInfo<R, T> fieldInfo =
                validator.getInfoOf(field);

        RandomGenerator random =
                randomFor(
                        field,
                        seedDelta
                );

        /*
         * Explicit allowed values are the strongest and easiest
         * possible generation rule.
         */
        var allowedValues =
                fieldInfo.allowedValues();

        if (allowedValues.isPresent()) {
            List<T> values = allowedValues.get();

            if (values.isEmpty()) {
                throw new IllegalStateException(
                        "Field "
                                + field.getQualifiedName()
                                + " declares an empty allowed-values set"
                );
            }

            return values.get(
                    random.nextInt(values.size())
            );
        }

        /*
         * Custom type generator takes precedence over the
         * built-in constraint-aware generation.
         */
        ValueGenerator<?> custom =
                customGenerators.get(
                        boxedType(field.getType())
                );

        if (custom != null) {
            return generateAndValidateCustom(
                    fieldInfo,
                    custom,
                    random
            );
        }

        /*
         * Standard constraints can be generated directly from
         * their domain.
         */
        for (int attempt = 0;
             attempt < MAX_ATTEMPTS;
             attempt++) {

            T value =
                    generateFromConstraint(
                            fieldInfo,
                            random
                    );

            if (fieldInfo.validate(value, false).isEmpty()) {
                return value;
            }
        }

        throw generationFailure(fieldInfo);
    }

    @SuppressWarnings("unchecked")
    private <T> T generateFromConstraint(
            FieldInfo<R, T> fieldInfo,
            RandomGenerator random
    ) {
        Constraint<T> constraint =
                fieldInfo.constraint;

        Object value;

        if (constraint instanceof ByteConstraint c) {

            value = (byte) randomIntInclusive(
                    random,
                    c.min(),
                    c.max()
            );

        } else if (constraint instanceof ShortConstraint c) {

            value = (short) randomIntInclusive(
                    random,
                    c.min(),
                    c.max()
            );

        } else if (constraint instanceof IntegerConstraint c) {

            value = randomIntInclusive(
                    random,
                    c.min(),
                    c.max()
            );

        } else if (constraint instanceof LongConstraint c) {

            value = randomLongInclusive(
                    random,
                    c.min(),
                    c.max()
            );

        } else if (constraint instanceof FloatConstraint c) {

            value = randomFloat(
                    random,
                    c.min(),
                    c.max()
            );

        } else if (constraint instanceof DoubleConstraint c) {

            value = randomDouble(
                    random,
                    c.min(),
                    c.max()
            );

        } else if (constraint instanceof BoolConstraint) {

            value = random.nextBoolean();

        } else if (constraint instanceof StringConstraint c) {

            int length = randomReasonableLength(
                    random,
                    c.minLength(),
                    c.maxLength()
            );

            value = randomString(
                    random,
                    length
            );

        } else if (constraint instanceof ByteArrayConstraint c) {

            int min =
                    c.minLength() == null
                            ? 0
                            : c.minLength();

            int max =
                    c.maxLength() == null
                            ? Integer.MAX_VALUE
                            : c.maxLength();

            int length =
                    randomReasonableLength(
                            random,
                            min,
                            max
                    );

            value = randomBytes(
                    random,
                    length
            );

        } else if (constraint instanceof LocalDateTimeConstraint c) {

            value = randomDateTime(
                    random,
                    c.minimum(),
                    c.maximum()
            );

        } else {
            throw new IllegalArgumentException(
                    "No value generator exists for constraint "
                            + constraint.getClass().getName()
                            + " on field "
                            + fieldInfo.field.getQualifiedName()
            );
        }

        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private <T> T generateAndValidateCustom(
            FieldInfo<R, T> fieldInfo,
            ValueGenerator<?> custom,
            RandomGenerator random
    ) {
        ValueGenerator<T> typedGenerator =
                (ValueGenerator<T>) custom;

        for (int attempt = 0;
             attempt < MAX_ATTEMPTS;
             attempt++) {

            T value =
                    typedGenerator.generate(random);

            if (fieldInfo.validate(value, false).isEmpty()) {
                return value;
            }
        }

        throw generationFailure(fieldInfo);
    }

    private RandomGenerator randomFor(
            TableField<R, ?> field,
            long seedDelta
    ) {
        long fieldHash =
                field.getQualifiedName()
                        .toString()
                        .hashCode();

        return new SplittableRandom(
                mix(seed)
                        ^ mix(seedDelta)
                        ^ mix(fieldHash)
        );
    }

    private static int randomIntInclusive(
            RandomGenerator random,
            int min,
            int max
    ) {
        if (min == max) {
            return min;
        }

        long range =
                (long) max
                        - min
                        + 1L;

        return (int) (
                min
                        + random.nextLong(range)
        );
    }

    private static long randomLongInclusive(
            RandomGenerator random,
            long min,
            long max
    ) {
        if (min == max) {
            return min;
        }

        if (
                min == Long.MIN_VALUE
                        && max == Long.MAX_VALUE
        ) {
            return random.nextLong();
        }

        long range =
                max
                        - min
                        + 1;

        /*
         * No overflow means we can sample directly.
         */
        if (range > 0) {
            return min
                    + random.nextLong(range);
        }

        /*
         * The interval occupies more than Long.MAX_VALUE values,
         * therefore rejection sampling has a good acceptance rate.
         */
        long value;

        do {
            value = random.nextLong();
        } while (
                value < min
                        || value > max
        );

        return value;
    }

    private static float randomFloat(
            RandomGenerator random,
            float min,
            float max
    ) {
        if (Float.compare(min, max) == 0) {
            return min;
        }

        double ratio =
                random.nextDouble();

        return (float) (
                (1.0 - ratio) * min
                        + ratio * max
        );
    }

    private static double randomDouble(
            RandomGenerator random,
            double min,
            double max
    ) {
        if (Double.compare(min, max) == 0) {
            return min;
        }

        double ratio =
                random.nextDouble();

        return (1.0 - ratio) * min
                + ratio * max;
    }

    /**
     * Don't create a 2GB String merely because maxLength is
     * Integer.MAX_VALUE.
     *
     * We generate within the first 32 valid lengths.
     */
    private static int randomReasonableLength(
            RandomGenerator random,
            int min,
            int max
    ) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException(
                    "Invalid length interval ["
                            + min
                            + ", "
                            + max
                            + "]"
            );
        }

        long practicalMax =
                Math.min(
                        (long) max,
                        (long) min + 32L
                );

        return randomIntInclusive(
                random,
                min,
                (int) practicalMax
        );
    }

    private static String randomString(
            RandomGenerator random,
            int length
    ) {
        StringBuilder result =
                new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(
                    STRING_CHARS[
                            random.nextInt(
                                    STRING_CHARS.length
                            )
                            ]
            );
        }

        return result.toString();
    }

    private static byte[] randomBytes(
            RandomGenerator random,
            int length
    ) {
        byte[] result =
                new byte[length];

        random.nextBytes(result);

        return result;
    }

    private static LocalDateTime randomDateTime(
            RandomGenerator random,
            LocalDateTime minimum,
            LocalDateTime maximum
    ) {
        if (
                minimum == null
                        && maximum == null
        ) {
            minimum =
                    LocalDateTime.of(
                            2000,
                            1,
                            1,
                            0,
                            0
                    );

            maximum =
                    LocalDateTime.of(
                            2050,
                            1,
                            1,
                            0,
                            0
                    );
        } else if (minimum == null) {

            minimum =
                    maximum.minusYears(50);

        } else if (maximum == null) {

            maximum =
                    minimum.plusYears(50);
        }

        long minSeconds =
                minimum.toEpochSecond(
                        ZoneOffset.UTC
                );

        long maxSeconds =
                maximum.toEpochSecond(
                        ZoneOffset.UTC
                );

        long seconds =
                randomLongInclusive(
                        random,
                        minSeconds,
                        maxSeconds
                );

        return LocalDateTime.ofEpochSecond(
                seconds,
                0,
                ZoneOffset.UTC
        );
    }

    private IllegalStateException generationFailure(
            FieldInfo<R, ?> fieldInfo
    ) {
        return new IllegalStateException(
                "Could not generate a valid value for field "
                        + fieldInfo.field.getQualifiedName()
                        + " after "
                        + MAX_ATTEMPTS
                        + " attempts"
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> boxedType(
            Class<T> type
    ) {
        if (!type.isPrimitive()) {
            return type;
        }

        if (type == byte.class)
            return (Class<T>) Byte.class;

        if (type == short.class)
            return (Class<T>) Short.class;

        if (type == int.class)
            return (Class<T>) Integer.class;

        if (type == long.class)
            return (Class<T>) Long.class;

        if (type == float.class)
            return (Class<T>) Float.class;

        if (type == double.class)
            return (Class<T>) Double.class;

        if (type == boolean.class)
            return (Class<T>) Boolean.class;

        if (type == char.class)
            return (Class<T>) Character.class;

        throw new IllegalArgumentException(
                "Unsupported primitive type "
                        + type.getName()
        );
    }

    private static long seedFrom(
            String value
    ) {
        return mix(value.hashCode());
    }

    private static long mix(
            long value
    ) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return value;
    }

    @FunctionalInterface
    public interface ValueGenerator<T> {

        T generate(RandomGenerator random);
    }
}