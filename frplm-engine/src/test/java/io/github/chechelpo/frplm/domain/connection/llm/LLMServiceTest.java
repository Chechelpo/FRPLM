package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.connection.api_hosts.HostTestContext;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({HostTestContext.class})
class LLMServiceTest {
    @Autowired
    HostTestContext hosts;
    @Autowired
    LLMService llmService;
    @Autowired
    LLMFieldsHelper fields;
    @BeforeEach
    void setUp(){
        hosts.reload();
    }

    @Test
    void assignHost_customOpenAI() {
        String newHostUrl = "https://host.com";
        LlmConnectionRecord con = llmService.createAndGet(EntityDataPayload.of(LLM_CONNECTION.NAME, "Test"));
        EntityKey<LlmConnectionRecord> conKey = llmService.keyOf(con);

        ApiHostsRecord newHost = llmService.assignHost(con.getId(), newHostUrl);

        LlmConnectionRecord actualCon = llmService.find(conKey).orElseThrow(Severity.USER);
        assertEquals(newHost.getId(), actualCon.getHostId().intValue());
        assertEquals(newHostUrl, newHost.getHostUrl());
    }
}