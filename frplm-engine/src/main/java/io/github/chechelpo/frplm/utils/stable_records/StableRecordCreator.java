package io.github.chechelpo.frplm.utils.stable_records;

public sealed interface StableRecordCreator permits StableRecordCreatorImpl {
    void run();
}
