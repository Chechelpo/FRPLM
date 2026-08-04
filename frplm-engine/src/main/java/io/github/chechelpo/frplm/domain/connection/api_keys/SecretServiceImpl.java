package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.utils.encryption.EncryptorService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
non-sealed class SecretServiceImpl extends EntityService<ApiKeysRecord, SecretStore> implements SecretService {
    private final EncryptorService encryptor;

    SecretServiceImpl(
            FieldValidator<ApiKeysRecord> validator,
            @NotNull EncryptorService encryptor,
            @NotNull SecretStore secretsStore,
            EventBus eventBus
    ) {
        super(secretsStore, validator, eventBus);
        this.encryptor = encryptor;
    }

    public @NotNull ApiKeysRecord registerNewKey(int host_id, @NotNull String key) {
        //We do not emit shit.
        String normalized = key.trim();

        if (normalized.length() >= 2
                && normalized.startsWith("\"")
                && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return store.newKey(host_id, encryptor.encrypt(normalized));
    }

    public @NotNull Optional<String> getKeyForConnectionHost(@NotNull LlmConnectionRecord record) throws EntityNotFound {
        List<ApiKeysRecord> records = store.ofHostID(record.getHostId());
        if (records.isEmpty()) {
            log.error("Could not find an api key for host id {}", record.getHostId());
            return Optional.empty();
        }

        ApiKeysRecord apiKeysRecord = records.getLast();
        return Optional.of(encryptor.decrypt(
                        apiKeysRecord.getApiKeyCiphertext(),
                        apiKeysRecord.getApiKeyNonce(),
                        apiKeysRecord.getApiKeyKeyVersion()
                )
        );
    }

    public boolean hasApiKey(@NotNull LlmConnectionRecord record) {
        return !store.ofHostID(record.getHostId()).isEmpty();
    }
}
