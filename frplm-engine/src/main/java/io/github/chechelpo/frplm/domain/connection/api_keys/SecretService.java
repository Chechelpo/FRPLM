package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public sealed interface SecretService permits SecretServiceImpl {
    @NotNull Optional<String> getKeyForConnectionHost(@NotNull LlmConnectionRecord record);
    boolean hasApiKey(@NotNull LlmConnectionRecord record);
}
