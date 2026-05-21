package chechelpo.frplm.domain.connection.llm.utils;

import chechelpo.frplm.domain.connection.api_hosts.HostService;
import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.microservices.LLMService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.springframework.stereotype.Component;

@Component
final class LLMRepository extends EntityRepository<LlmConnectionRecord, LLMService> {
    private final HostService hosts;
    private final SecretService secrets;

    LLMRepository(HostService hosts, LLMService llms, SecretService secrets) {
        super(llms);
        this.hosts = hosts;
        this.secrets = secrets;
    }

    boolean exists(EntityKey<LlmConnectionRecord> key){
        return service.exists(key);
    }
    LlmConnectionRecord require(EntityKey<LlmConnectionRecord> key){
        return service.require(key);
    }
    @NotNull String getDecryptedKey(EntityKey<ApiKeysRecord> key){
        return secrets.getKey(key);
    }
    <T> T getFromLLM(TableField<LlmConnectionRecord, T> field, EntityKey<LlmConnectionRecord> key){
        return service.getValueOf(field, key);
    }
    <T> T getFromHosts(TableField<ApiHostsRecord, T> field, EntityKey<ApiHostsRecord> key){
        return hosts.getValueOf(field, key);
    }
    <T> T getFromSecrets(TableField<ApiKeysRecord, T> field, EntityKey<ApiKeysRecord> key){
        return secrets.getValueOf(field, key);
    }
}
