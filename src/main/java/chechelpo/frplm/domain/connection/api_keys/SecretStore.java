package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.utils.encryption.EncryptedSecret;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.API_KEYS;

@Component
final class SecretStore extends ABSEntityStore<ApiKeysRecord> {

    SecretStore(@NotNull DSLContext dsl) {
        super(dsl, API_KEYS, EntityTypes.Types.API_KEYS);
    }

    public ApiKeysRecord newKey(@NotNull String name, @NotNull EncryptedSecret secret){
        ApiKeysRecord key = ctx
                .insertInto(API_KEYS)
                .set(API_KEYS.NAME, name)
                .set(API_KEYS.API_KEY_CIPHERTEXT, secret.ciphertext())
                .set(API_KEYS.API_KEY_KEY_VERSION, secret.keyVersion())
                .set(API_KEYS.API_KEY_NONCE, secret.nonce())
                .returning()
                .fetchOne();
        if (key == null) throw new IllegalStateException("Error when inserting key");

        return key;
    }
}
