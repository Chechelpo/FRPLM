package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({HostTestContext.class})
class HostServiceTest {
    @Autowired HostTestContext hostTestContext;

    @BeforeEach
    void setUp() {
        hostTestContext.reload();
    }

    @Test
    void testLLMBackendsRegistered(){
        for (LLMBackend backend : LLMBackend.values()){
            if (backend.toKey().isPresent()){
                Optional<ApiHostsRecord> record = hostTestContext.service.find(backend.toKey().get());
                assertTrue(record.isPresent());
                ApiHostsRecord llmBackendRecord = record.get();

                assertEquals(backend.stable_id, llmBackendRecord.getId(), "Mismatch in backend id for: " + backend);
                assertNotNull(backend.host, "Standard backend host is null for: " + backend);
                assertEquals(backend.host.toString(), llmBackendRecord.getHostUrl(),
                        "Mismatch in backend host url for: " + backend
                );
            }
        }
    }
    @Test
    void testCannotDeleteStandardBackend(){
        for (LLMBackend backend : LLMBackend.values()){
            if (backend.toKey().isPresent())
                assertFalse(hostTestContext.service.delete(backend.toKey().get()), "Could delete backend: " + backend);
        }
    }

    @Test
    void createOrGetWithHost_DoesNotDuplicate(){
        String newHostUrl = "https://host.com";

        hostTestContext.service.createOrGetWithHost(newHostUrl);
        hostTestContext.service.createOrGetWithHost(newHostUrl);

        List<ApiHostsRecord> allCon = hostTestContext.service.getAll();
        assertEquals(LLMBackend.getIDs().length + 1, allCon.size());
    }
}