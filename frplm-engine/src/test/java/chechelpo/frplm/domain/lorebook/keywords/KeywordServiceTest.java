package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static chechelpo.frplm.jooq.generated.Tables.KEYWORD;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KeywordServiceTest {
    @Autowired KeywordService keywordService;
    @Autowired KeywordFieldHelper fields;

    @Test
    void getOrGenerate_whenKeywordDoesNotExist_createsKeyword() {
        String keyword = "test_keyword_" + System.nanoTime();

        int id = keywordService.getOrGenerate(keyword);

        assertTrue(id > 0);
        assertTrue(keywordService.existsWith(keyword));
        assertEquals(id, keywordService.getIDOfKeywordWith(keyword));
    }

    @Test
    void getOrGenerate_whenKeywordAlreadyExists_returnsSameID() {
        String keyword = "test_keyword_" + System.nanoTime();

        int firstID = keywordService.getOrGenerate(keyword);
        int secondID = keywordService.getOrGenerate(keyword);

        assertEquals(firstID, secondID);
        assertTrue(keywordService.existsWith(keyword));
    }

    @Test
    void update_alwaysThrowsUnsupportedOperationException() {
        EntityKey<KeywordRecord> key = EntityKey.of(KEYWORD.ID, 1);
        EntityDataPayload<KeywordRecord> payload = EntityDataPayload.<KeywordRecord>builder()
                .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> keywordService.update(key, payload)
        );
    }
}