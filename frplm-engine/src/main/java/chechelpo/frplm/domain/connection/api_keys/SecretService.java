package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.utils.encryption.EncryptorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SecretService extends EntityService<ApiKeysRecord, SecretStore> {
    private final EncryptorService encryptor;

    SecretService(@NotNull EncryptorService encryptor, @NotNull SecretStore secretsStore, EventBus eventBus) {
        super(secretsStore, eventBus);
        this.encryptor = encryptor;
    }

    @Override
    protected void beforeCreate(EntityDataPayload<ApiKeysRecord> data, long operationID) {
        log.error("Secrets can't be created normally");
        throw new UnsupportedOperationException("Secrets can't be created through normal framework");
    }

    @Override
    protected void beforeRetrieve(@Nullable EntityKey<ApiKeysRecord> key, boolean isFullKey, long operationID) {
        log.error("Secrets can't be retrieved normally");
        throw new UnsupportedOperationException("Secrets can't be retrieved normally");
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<ApiKeysRecord> target, EntityDataPayload<ApiKeysRecord> data, long operationID) {
        log.error("Secrets can't be updated normally");
        throw new UnsupportedOperationException("Secrets can't be updated normally");
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

        ApiKeysRecord apiKeysRecord = records.getFirst();
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
