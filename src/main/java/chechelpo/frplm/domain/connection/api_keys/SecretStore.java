package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.utils.encryption.EncryptedSecret;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.API_KEYS;

@Component
final class SecretStore extends ABSEntityStore<ApiKeysRecord> {

    SecretStore(@NotNull DSLContext dsl) {
        super(dsl, API_KEYS, EntityTypes.Types.API_KEYS);
    }

    public ApiKeysRecord newKey(int host_id, @NotNull EncryptedSecret secret){
        ApiKeysRecord key = ctx
                .insertInto(API_KEYS)
                .set(API_KEYS.HOST_ID, host_id)
                .set(API_KEYS.API_KEY_CIPHERTEXT, secret.ciphertext())
                .set(API_KEYS.API_KEY_KEY_VERSION, secret.keyVersion())
                .set(API_KEYS.API_KEY_NONCE, secret.nonce())
                .returning()
                .fetchOne();
        if (key == null) throw new IllegalStateException("Error when inserting key");

        return key;
    }

    @Contract("_ -> new")
    public @NotNull EncryptedSecret getSecret(@NotNull EntityKey<ApiKeysRecord> key){
        ApiKeysRecord record = this.get(key);
        if (record == null) throw new IllegalStateException("Error when getting key");
        return new EncryptedSecret(
                record.get(API_KEYS.API_KEY_CIPHERTEXT),
                record.get(API_KEYS.API_KEY_NONCE),
                record.get(API_KEYS.API_KEY_KEY_VERSION)
        );
    }
}
