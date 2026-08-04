package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.utils.stable_records.StableRecord;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordProvider;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
class LLMBackendProvider implements StableRecordProvider {
    @Override
    public Collection<? extends StableRecord<?>> stableRecords() {
        return List.of(LLMBackend.values());
    }
}
