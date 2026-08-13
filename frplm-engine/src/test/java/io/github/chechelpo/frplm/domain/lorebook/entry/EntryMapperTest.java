package io.github.chechelpo.frplm.domain.lorebook.entry;

import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletServiceTestFactory;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.orders.NewEntryOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EntryKeywordService entryKeywordService;

    private OutletService outletService;

    private EntryMapper mapper;

    @BeforeEach
    void setUp() {
        outletService = OutletServiceTestFactory.mockService();
        mapper = new EntryMapper(
                OBJECT_MAPPER,
                entryKeywordService,
                outletService
        );
    }

    @Test
    void testRoundTrip() {
        EntryRecord entry = new EntryRecord();

        entry.set(ENTRY.LOREBOOK_ID, 1);
        entry.set(ENTRY.ENTRY_ID, 2);

        entry.set(ENTRY.NAME, "entry");
        entry.set(ENTRY.CONTENT, "content");
        entry.set(ENTRY.EMBED_TEXT, "embed text");

        entry.set(ENTRY.OUTLET, 3);

        entry.set(ENTRY.ENABLED, true);
        entry.set(ENTRY.PROBABILITY, (short) 75);
        entry.set(ENTRY.DELAY, 2);
        entry.set(ENTRY.COOLDOWN, 3);
        entry.set(ENTRY.STICK_THROUGH, 4);
        entry.set(ENTRY.POSITION, (short) 5);
        entry.set(ENTRY.STRATEGY, (short) 1);

        entry.set(ENTRY.PREVENT_FURTHER_RECURSION, true);
        entry.set(ENTRY.NON_RECURSABLE, false);
        entry.set(ENTRY.DELAY_UNTIL_RECURSION, true);

        entry.set(ENTRY.SCAN_DEPTH, (short) 6);
        entry.set(ENTRY.GROUP_ID, (short) 7);

        Set<String> keywords = Set.of(
                "keyword1",
                "keyword2"
        );

        when(entryKeywordService.keywordsOfEntry(1, 2))
                .thenReturn(keywords);

        when(outletService.getOutletName(3))
                .thenReturn(Optional.of("outlet"));

        when(outletService.getOrCreateOutlet("outlet"))
                .thenReturn(3);

        NewEntryOrder order =
                mapper.orderFrom(mapper.jsonRecordFrom(entry, null));

        Asserts.assertRecordEqualsPayloadMinusFields(
                entry,
                order.payload(),
                Set.of(
                        ENTRY.LOREBOOK_ID,
                        ENTRY.ENTRY_ID
                )
        );

        assertEquals(keywords, order.keywords());
    }
}