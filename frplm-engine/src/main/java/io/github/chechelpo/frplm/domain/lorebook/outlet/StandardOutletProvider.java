package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.utils.stable_records.StableRecord;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordProvider;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
final class StandardOutletProvider implements StableRecordProvider {

    @Override
    public Collection<? extends StableRecord<?>> stableRecords() {
        return List.of(StandardOutlet.values());
    }
}