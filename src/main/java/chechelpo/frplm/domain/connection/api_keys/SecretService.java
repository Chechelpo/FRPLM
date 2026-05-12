package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.utils.encryption.EncryptorService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.swing.text.html.parser.Entity;

@Service
public final class SecretService extends ABSEntityService<ApiKeysRecord, SecretStore> {
    private final EncryptorService encryptor;

    SecretService(@NotNull EncryptorService encryptor, @NotNull SecretStore secretsStore) {
        super(secretsStore);
        this.encryptor = encryptor;
    }

    @Override
    protected EntityDataPayload<ApiKeysRecord> beforeCreate(EntityDataPayload<ApiKeysRecord> data) {
        log.error("Secrets can't be created normally");
        throw new UnsupportedOperationException("Secrets can't be created through normal framework");
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

    public @NotNull String getKey(EntityKey<ApiKeysRecord> key) {
        return encryptor.decrypt(store.getSecret(key));
    }
}
