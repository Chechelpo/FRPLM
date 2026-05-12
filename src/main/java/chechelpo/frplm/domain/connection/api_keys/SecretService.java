package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.utils.encryption.EncryptorService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public final class SecretService extends ABSEntityService<ApiKeysRecord, SecretStore> {
    private final EncryptorService encryptor;

    SecretService(@NotNull EncryptorService encryptor, @NotNull SecretStore secretsStore) {
        super(secretsStore, EntityTypes.Types.API_KEYS);
        this.encryptor = encryptor;
    }

    @Override
    protected EntityDataPayload<ApiKeysRecord> beforeCreate(EntityDataPayload<ApiKeysRecord> data) {
        log.error("Secrets can't be created normally");
        throw new UnsupportedOperationException("Secrets can't be created through normal framework");
    }

    public @NotNull ApiKeysRecord registerNewKey(@NotNull String name, @NotNull String key){
        //We do not emit shit.
        return store.newKey(name, encryptor.encrypt(key));
    }
}
