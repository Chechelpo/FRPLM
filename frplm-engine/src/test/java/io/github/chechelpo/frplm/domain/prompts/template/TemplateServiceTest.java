package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.connection.llm.LLMTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;
import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;
import static org.junit.jupiter.api.Assertions.*;

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
        PromptTemplateRecord promptTemplate = promptTestContext.service.createAndGet(
                EntityDataPayload.of(PROMPT_TEMPLATE.NAME, "test")
        );
        LlmConnectionRecord llmConnectionRecord = llmTestContext.service.createAndGet(
                EntityDataPayload.<LlmConnectionRecord>builder()
                        .set(LLM_CONNECTION.NAME, "test")
                        .set(LLM_CONNECTION.MAX_TOKENS, maxTokens)
                        .build()
        );

        promptTestContext.service.update(
                promptTestContext.service.keyOf(promptTemplate),
                EntityDataPayload.of(PROMPT_TEMPLATE.CONNECTION_ID, llmConnectionRecord.getId())
        );

        assertThrows(
                InvalidValue.class,
                () -> promptTestContext.service.update(
                        promptTestContext.service.keyOf(promptTemplate),
                        EntityDataPayload.of(PROMPT_TEMPLATE.MAX_TOKENS, maxTokens + 1)
                )
        );
        assertDoesNotThrow(
                () -> promptTestContext.service.update(
                        promptTestContext.service.keyOf(promptTemplate),
                        EntityDataPayload.of(PROMPT_TEMPLATE.MAX_TOKENS, maxTokens)
                )
        );
    }

    @Test
    void onConnectionModelUpdate_reloadsPromptTemplateMaxTokens(){
        int maxTokens = 200;
        PromptTemplateRecord promptTemplate = promptTestContext.service.createAndGet(
                EntityDataPayload.of(PROMPT_TEMPLATE.NAME, "test")
        );
        LlmConnectionRecord llmConnectionRecord = llmTestContext.service.createAndGet(
                EntityDataPayload.<LlmConnectionRecord>builder()
                        .set(LLM_CONNECTION.NAME, "test")
                        .set(LLM_CONNECTION.MODEL, "testModel")
                        .set(LLM_CONNECTION.MAX_TOKENS, maxTokens)
                        .build()
        );
        EntityKey<PromptTemplateRecord> promptKey = promptTestContext.service.keyOf(promptTemplate);
        EntityKey<LlmConnectionRecord> connectionKey = llmTestContext.service.keyOf(llmConnectionRecord);

        promptTestContext.service.update(promptKey, EntityDataPayload.of(PROMPT_TEMPLATE.CONNECTION_ID, llmConnectionRecord.getId()));
        llmTestContext.service.update(connectionKey, EntityDataPayload.of(LLM_CONNECTION.MAX_TOKENS, maxTokens - 2));

        Optional<PromptTemplateRecord> newRecord = promptTestContext.service.find(promptKey);

        assertTrue(newRecord.isPresent());
        assertEquals(maxTokens - 2, newRecord.get().getMaxTokens());
    }

    @Test
    void onUpdate_throwsIfInvalidBudget(){
        PromptTemplateRecord promptTemplate = promptTestContext.service.createAndGet(
                EntityDataPayload.of(PROMPT_TEMPLATE.NAME, "test")
        );
        EntityKey<PromptTemplateRecord> key = promptTestContext.service.keyOf(promptTemplate);
        float incorrectChatHistoryBudget = 0.5f;
        float incorrectLorebookBudget = 0.7f;

        assertThrows(
                InvalidValue.class,
                () -> promptTestContext.service.update(
                        key,
                        EntityDataPayload.<PromptTemplateRecord>builder()
                                .set(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET, incorrectChatHistoryBudget)
                                .set(PROMPT_TEMPLATE.LOREBOOKS_BUDGET, incorrectLorebookBudget)
                                .build()
                )
        );
    }

    @Test
    void onCreate_throwsIfInvalidBudget(){
        assertThrows(
                InvalidValue.class,
                () -> promptTestContext.service.createAndGet(
                        EntityDataPayload.<PromptTemplateRecord>builder()
                                .set(PROMPT_TEMPLATE.NAME, "Test")
                                .set(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET, 0.5f)
                                .set(PROMPT_TEMPLATE.LOREBOOKS_BUDGET, 0.6f)
                                .build()
                )
        );
    }
}