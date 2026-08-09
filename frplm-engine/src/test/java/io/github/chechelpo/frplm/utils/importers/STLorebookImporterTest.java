package io.github.chechelpo.frplm.utils.importers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryTestContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.orders.NewEntryOrder;
import io.github.chechelpo.frplm.utils.importers.sillytavern.STLorebookImporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(EntryTestContext.class)
class STLorebookImporterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired
    private EntryTestContext entryTestContext;

    @Test
    void getEntries() throws IOException {
        InputStream in = STLorebookImporterTest.class.getResourceAsStream("/imports/st_lorebooks/eldoria.json");
        assert in != null : "Resource not found: /imports/eldoria.json";
        JsonNode testLorebook = MAPPER.readTree(in);
        System.out.println(testLorebook);

        List<NewEntryOrder> entryOrders = STLorebookImporter.getEntries(testLorebook);
        System.out.println("Results: " + entryOrders);
        LorebooksRecord lorebook = entryTestContext.lorebooks.createLorebooks(0L, 1).getFirst();

        entryTestContext.entryService.importEntriesFromJSON(lorebook.getId(), testLorebook);

        List<EntryRecord> entries = entryTestContext.entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, lorebook.getId()));

        for (NewEntryOrder order : entryOrders){
            String expectedName = order.payload().require(ENTRY.NAME);
            String expectedContent = order.payload().require(ENTRY.CONTENT);
            Set<String> expectedKeywords = order.keywords();
            assertTrue(
                    entries.stream().anyMatch(record -> record.getName().equals(expectedName)),
                    "No entry found with name: " + expectedName + " actual entries: \n" + entries
            );
            assertTrue(
                    entries.stream().anyMatch(record -> record.getContent().equals(expectedContent)),
                    "No entry with content: " + expectedContent + " actual entries: \n" + entries
            );
            assertTrue(
                    entries.stream().anyMatch(record ->
                            entryTestContext.entryKeywords.entryKeywordsService.keywordsOfEntry(record.getLorebookId(), record.getEntryId())
                                    .equals(expectedKeywords)
                    ),
                    "No entry with keywords " + expectedContent
            );
        }
    }
}