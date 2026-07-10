package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.utils.encryption.EncryptedSecret;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.API_KEYS;

@Component
final class SecretStore extends EntityStore<ApiKeysRecord> {

    SecretStore(@NotNull DSLContext dsl) {
        super(dsl, API_KEYS, EntityConfigs.Types.API_KEYS);
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

    public @NotNull List<ApiKeysRecord> ofHostID(int host_id){
        return ctx.selectFrom(API_KEYS)
                .where(API_KEYS.HOST_ID.eq(host_id))
                .orderBy(API_KEYS.KEY_ID.desc())
                .fetch();
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
