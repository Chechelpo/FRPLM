package chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.core.entities.fields.coercers.Coercer;
import chechelpo.frplm.utils.format.Either;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.HashMap;
import java.util.Map;

final class DataFormatter {
    private DataFormatter() {}
    enum ERROR_TYPE {
        UNKNOWN_FIELD,
        COERCER_ERROR
    }
    record FormatError<R extends TableRecord<R>>(ERROR_TYPE type, String message) {
        @Contract(pure = true)
        public @NotNull String getMessage(){
            return "Format error: " + type +
                    "\nMESSAGE: " + message;
        }
    }

    /**
     * @implNote rightOrThrow() is fine here cause of the first check, it shouldn't ever throw.
     */
    @Contract(pure = true)
    static <R extends TableRecord<R>> @NotNull Either<FormatError<R>, Map<TableField<R,?>, Object>> coerceValues(
            @NotNull Map<String, Object> params,
            Map<String, TableField<R, ?>> translator,
            Map<TableField<R,?>, Coercer<?>> coercers
    ) {
        HashMap<TableField<R, ?>, Object> result = new HashMap<>(params.size());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!translator.containsKey(entry.getKey()))
                return Either.left(new FormatError<>(ERROR_TYPE.UNKNOWN_FIELD, "Unknown field: " + entry.getKey()));

            TableField<R, ?> field = translator.get(entry.getKey());
            Coercer<?> coercer = coercers.get(field);
            Either<Coercer.CoerceError, ?> coerceResult = coercer.coerce(entry.getValue());
            if (coerceResult.isLeft())
                return Either.left(new FormatError<>(ERROR_TYPE.COERCER_ERROR, coerceResult.leftOrThrow().message()));

            result.put(field, coerceResult.rightOrThrow());
        }

        return Either.right(result);
    }


}
