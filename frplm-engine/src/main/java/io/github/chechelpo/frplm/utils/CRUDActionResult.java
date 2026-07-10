package io.github.chechelpo.frplm.utils;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableRecord;

public sealed interface CRUDActionResult<T, R extends TableRecord<R>> {
    record Success<T, R extends TableRecord<R>>(T result) implements CRUDActionResult<T,R> {}
    @Contract("_ -> new")
    static <T, R extends TableRecord<R>> CRUDActionResult.@NotNull Success<T, R> success(T object){
        return new CRUDActionResult.Success<>(object);
    }

    record EntityNotFound<T, R extends TableRecord<R>>
            (Class<?> issuer, String message, @Nullable EntityKey<R> entityKey) implements CRUDActionResult<T,R> {}

    record InvalidValue<T, R extends TableRecord<R>>
            (Class<?> issuer, String message, Object value) implements CRUDActionResult<T, R> {}
}
