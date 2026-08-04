package io.github.chechelpo.frplm.core.entities.fields.coercers;

import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CoercerCreator {
    private CoercerCreator() {}

    private static final Map<Class<?>, Coercer<?>> COERCERS;

    static {
        Map<Class<?>, Coercer<?>> coercers = new HashMap<>();

        register(byte.class, ByteCoercer.instance, coercers);
        register(Byte.class, ByteCoercer.instance, coercers);

        register(byte[].class, ByteArrayCoercer.instance, coercers);

        register(short.class, ShortCoercer.instance, coercers);
        register(Short.class, ShortCoercer.instance, coercers);

        register(int.class, IntegerCoercer.instance, coercers);
        register(Integer.class, IntegerCoercer.instance, coercers);

        register(long.class, LongCoercer.instance, coercers);
        register(Long.class, LongCoercer.instance, coercers);

        register(float.class, FloatCoercer.instance, coercers);
        register(Float.class, FloatCoercer.instance, coercers);

        register(double.class, DoubleCoercer.instance, coercers);
        register(Double.class, DoubleCoercer.instance, coercers);

        register(boolean.class, BoolCoercer.instance, coercers);
        register(Boolean.class, BoolCoercer.instance, coercers);

        register(String.class, StringCoercer.instance, coercers);

        register(LocalDateTime.class, LocalDateTimeCoercer.instance, coercers);

        COERCERS = Map.copyOf(coercers);
    }

    private static <T> void register(
            Class<T> clazz,
            Coercer<T> coercer,
            Map<Class<?>, Coercer<?>> coercers
    ) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(coercer, "coercer");

        Coercer<?> previous = coercers.putIfAbsent(clazz, coercer);

        if (previous != null) {
            throw new IllegalStateException(
                    "A coercer is already registered for " + clazz.getName()
            );
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> @NonNull Coercer<T> getCoercerForClass(Class<T> clazz) {
        Coercer<?> coercer = COERCERS.get(clazz);

        if (coercer == null) {
            throw new IllegalArgumentException(
                    "No coercer registered for " + clazz.getName()
            );
        }

        return (Coercer<T>) coercer;
    }
}
