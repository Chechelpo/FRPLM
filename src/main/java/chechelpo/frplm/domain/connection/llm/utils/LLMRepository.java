package chechelpo.frplm.domain.connection.llm.utils;

import chechelpo.frplm.domain.connection.api_hosts.HostService;
import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.microservices.LLMService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.springframework.stereotype.Component;

@Component
final class LLMRepository {
    private final HostService hosts;
    private final LLMService llms;
    private final SecretService secrets;

    LLMRepository(HostService hosts, LLMService llms, SecretService secrets) {
        this.hosts = hosts;
        this.llms = llms;
        this.secrets = secrets;
    }

    boolean exists(EntityKey<LlmConnectionRecord> key){
        return llms.exists(key);
    }
    LlmConnectionRecord require(EntityKey<LlmConnectionRecord> key){
        return llms.require(key);
    }
    @NotNull String getDecryptedKey(EntityKey<ApiKeysRecord> key){
        return secrets.getKey(key);
    }
    <T> T getFromLLM(TableField<LlmConnectionRecord, T> field, EntityKey<LlmConnectionRecord> key){
        return llms.get(field, key);
    }
    <T> T getFromHosts(TableField<ApiHostsRecord, T> field, EntityKey<ApiHostsRecord> key){
        return hosts.get(field, key);
    }
    <T> T getFromSecrets(TableField<ApiKeysRecord, T> field, EntityKey<ApiKeysRecord> key){
        return secrets.get(field, key);
    }
}
