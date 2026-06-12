package chechelpo.frplm.domain.prompts.template;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.connection.llm.LLMTestContext;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({PromptTemplateTestContext.class, LLMTestContext.class})
class TemplateServiceTest {
    @Autowired PromptTemplateTestContext promptTestContext;
    @Autowired LLMTestContext llmTestContext;

    @BeforeEach
    void setUp() {}

    @Test
    void updateTemplate_cannotSurpassMaxTokensOfConnection(){
        int maxTokens = 200;
        PromptTemplateRecord promptTemplate = promptTestContext.templates.createAndGet(
                EntityDataPayload.of(PROMPT_TEMPLATE.NAME, "test")
        );
        LlmConnectionRecord llmConnectionRecord = llmTestContext.service.createAndGet(
                EntityDataPayload.<LlmConnectionRecord>builder()
                        .set(LLM_CONNECTION.NAME, "test")
                        .set(LLM_CONNECTION.MAX_TOKENS, maxTokens)
                        .build()
        );

        promptTestContext.templates.update(
                promptTestContext.templates.keyOf(promptTemplate),
                EntityDataPayload.of(PROMPT_TEMPLATE.CONNECTION_ID, llmConnectionRecord.getId())
        );

        assertThrows(
                InvalidValue.class,
                () -> promptTestContext.templates.update(
                        promptTestContext.templates.keyOf(promptTemplate),
                        EntityDataPayload.of(PROMPT_TEMPLATE.MAX_TOKENS, maxTokens + 1)
                )
        );
        assertDoesNotThrow(
                () -> promptTestContext.templates.update(
                        promptTestContext.templates.keyOf(promptTemplate),
                        EntityDataPayload.of(PROMPT_TEMPLATE.MAX_TOKENS, maxTokens)
                )
        );
    }

    @Test
    void updateTemplate_cannotChangeConnectionIDToNoModel(){}
}