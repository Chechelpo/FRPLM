package chechelpo.frplm.domain.secrets;

import chechelpo.frplm.utils.EncryptorService;
import chechelpo.frplm.utils.encryption.EncryptedSecret;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
final class SecretsStore {
    private final DSLContext ctx;
    private final EncryptorService encryptorService = null;
    SecretsStore(DSLContext dsl) {
        this.ctx = dsl;
    }
    public void storeKey(@NotNull EncryptedSecret secret){
    }
}
