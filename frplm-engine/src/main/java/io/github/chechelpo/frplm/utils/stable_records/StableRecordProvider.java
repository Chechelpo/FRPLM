package io.github.chechelpo.frplm.utils.stable_records;

import java.util.Collection;

public interface StableRecordProvider {
    Collection<? extends StableRecord<?>> stableRecords();
}