package io.github.chechelpo.frplm.utils.collections;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class IntSetFactory {
    private IntSetFactory() {}
    private static final int INT_ARRAY_SET_BOUNDARY = 100;

    /** @implNote decides on an Intset implementation depending on the number of integers in the list */
    @Contract(value = "null -> null", pure = true)
    public static IntSet ofValues(List<Integer> list) {
        if (list == null) return null;
        if (list.isEmpty()) return IntSet.of();

        if (list.size() < INT_ARRAY_SET_BOUNDARY) return new IntArraySet(list);
        return new IntOpenHashSet(list);
    }

    /** @implNote decides on an Intset implementation depending on the number of integers in the list */
    @Contract(value = "null -> null", pure = true)
    public static IntSet ofValues(int ...  arr ) {
        if (arr == null) return null;
        if (arr.length == 0) return IntSet.of();

        if (arr.length < INT_ARRAY_SET_BOUNDARY) return new IntArraySet(arr);
        return new IntOpenHashSet(arr);
    }

    @Contract(pure = true)
    public static IntSet ofLength(int length) {
        if (length == 0) return IntSet.of();
        if (length < INT_ARRAY_SET_BOUNDARY) return new IntArraySet(length);
        return new IntOpenHashSet(length);
    }
}
