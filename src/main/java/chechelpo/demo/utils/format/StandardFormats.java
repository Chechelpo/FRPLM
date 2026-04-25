package chechelpo.demo.utils.format;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Map;

/** Utility functions for formatting things into Strings */
public final class StandardFormats {
    private StandardFormats() {}

    /**
     * @param args a series of keys and their value.
     * @throws IllegalArgumentException if args is empty
     * @return a string with format "(args[0]=value_0,args[1]=value_1,...,args[n]=value_n)"
     * @implNote O(n)
     */
    @Contract(pure = true)
    public static <R extends TableRecord<R>> @NotNull String formatIDUnion(@NotNull Map<TableField<R, ?>, Object> args){
        if (args.isEmpty()) throw new IllegalArgumentException("Empty argument array");

        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (Map.Entry<TableField<R, ?>, Object> entry : args.entrySet()) {
            if (!first) result.append(",");
            result.append(entry.getKey().toString() + "=" + entry.getValue().toString());
            first = false;
        }

        return result.toString();
    }
}
