package chechelpo.frplm.utils.collections;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class IntSetFactory {
    private IntSetFactory() {}
    /** @implNote decides on an Intset implementation depending on the number of integers in the list */
    @Contract(value = "null -> null", pure = true)
    public static IntSet ofValues(List<Integer> list) {
        if (list == null) return null;
        if (list.isEmpty()) return IntSet.of();

        if (list.size() < 100) return new IntArraySet(list);
        return new IntOpenHashSet(list);
    }
}
