package io.github.chechelpo.frplm.domain.lorebook.outlet;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.jooq.Record2;
import org.jooq.Result;

import java.util.Optional;

public sealed interface OutletService permits OutletServiceImpl {
    Optional<Integer> getOutletID(String value);
    Optional<String> getOutletName(int id);

    int getOrCreateOutlet(String name);

    Result<Record2<Integer, String>> getOutletsFromIds(IntSet outletIds);
}
