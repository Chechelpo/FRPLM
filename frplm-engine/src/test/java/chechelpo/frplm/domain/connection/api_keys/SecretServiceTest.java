package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.utils.encryption.EncryptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.API_KEYS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({SecretServiceTestContext.class})
class SecretServiceTest {
    @Autowired SecretServiceTestContext secrets;

    @BeforeEach
    void setUp() {
        secrets.reload();
    }

    @Test
    void throwsOnNormalOperation() {
        String apiKey = EncryptorService.generateBase64Key();
        ApiKeysRecord record = secrets.secretService.registerNewKey(LLMBackend.NANOGPT.stable_id, apiKey);
        EntityKey<ApiKeysRecord> apiEntityKey = EntityKey.of(API_KEYS.KEY_ID, record.getKeyId());

        assertThrows(UnsupportedOperationException.class, () -> secrets.secretService.createAndGet(
                EntityDataPayload.of(API_KEYS.API_KEY_KEY_VERSION, 3))
        );
        assertThrows(UnsupportedOperationException.class, () -> secrets.secretService.find(apiEntityKey));
        assertThrows(UnsupportedOperationException.class, () -> secrets.secretService.update(apiEntityKey,
                EntityDataPayload.of(API_KEYS.API_KEY_KEY_VERSION, 2))
        );
    }

    @Test
    void getKeyForConnectionHost() {
        String apiKey = EncryptorService.generateBase64Key();
        ApiKeysRecord record = secrets.secretService.registerNewKey(LLMBackend.NANOGPT.stable_id, apiKey);
        LlmConnectionRecord llmConnectionRecord = new LlmConnectionRecord();

        llmConnectionRecord.setHostId(LLMBackend.NANOGPT.stable_id.shortValue());

        Optional<String> fectchedOptional = secrets.secretService.getKeyForConnectionHost(llmConnectionRecord);
        assertTrue(fectchedOptional.isPresent());
        assertEquals(apiKey, fectchedOptional.get());
    }

    @Test
    void hasApiKey() {
        String apiKey = EncryptorService.generateBase64Key();
        ApiKeysRecord record = secrets.secretService.registerNewKey(LLMBackend.NANOGPT.stable_id, apiKey);
        LlmConnectionRecord llmConnectionRecord = new LlmConnectionRecord();

        llmConnectionRecord.setHostId(LLMBackend.NANOGPT.stable_id.shortValue());

        assertTrue(secrets.secretService.hasApiKey(llmConnectionRecord));
    }
}