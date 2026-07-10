package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryTestContext;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordsTestContext;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewEntryOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({EntryTestContext.class, EntryKeywordsTestContext.class})
class EntryMapperTest {
    @Autowired
    EntryTestContext entryContext;
    @Autowired
    EntryKeywordsTestContext entryKeywordsTestContext;
    @Autowired
    KeywordService keywords;
    @Autowired
    EntryMapper mapper;

    @BeforeEach
    void setUp(){
        entryContext.reload();
    }

    @Test
    void testRoundtrip(){
        Map<LorebooksRecord, List<EntryRecord>> testContext = entryContext.createEntries(10L, 1, 1);
        EntryRecord relevantEntry = testContext.values().stream().toList().getFirst().getFirst();

        String keywordA = "keywordA";
        String keywordB = "keywordB";
        String nonRegisteredKeyword = "ignored";

        keywords.getOrGenerate(nonRegisteredKeyword);
        entryKeywordsTestContext.entryKeywordsService.associate(relevantEntry.getLorebookId(), relevantEntry.getEntryId(), keywordA);
        entryKeywordsTestContext.entryKeywordsService.associate(relevantEntry.getLorebookId(), relevantEntry.getEntryId(), keywordB);

        JsonNode node = mapper.jsonFrom(relevantEntry);
        NewEntryOrder order = mapper.orderFrom(node);

        assertEquals(Set.of(keywordA, keywordB), order.keywords(), "Mismatch in keywords");

        String actualName = order.entryInfo().requireValue(ENTRY.NAME);
        String actualContent = order.entryInfo().requireValue(ENTRY.CONTENT);

        assertEquals(relevantEntry.getName(), actualName);
        assertEquals(relevantEntry.getContent(), actualContent);
    }
}