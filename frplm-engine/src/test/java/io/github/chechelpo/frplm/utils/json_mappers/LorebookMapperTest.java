package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletServiceTestFactory;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewLorebookOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LorebookMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OutletService outletService;
    private EntryService entryService;
    private EntryMapper entryMapper;

    @BeforeEach
    void setUp(){
        outletService = OutletServiceTestFactory.mockService();
        entryService = mock(EntryService.class);
        entryMapper = mock(EntryMapper.class);
    }

    @Test
    void testRoundtrip(){
        int outletId = 1;
        String outletName = "outlet";

        LorebooksRecord record = new LorebooksRecord();
        record.set(LOREBOOKS.ID, 1);
        record.set(LOREBOOKS.NAME, "lorebook");
        record.set(LOREBOOKS.DEFAULT_OUTLET_ID, outletId);

        when(outletService.getOutletName(outletId))
                .thenReturn(Optional.of(outletName));
        when(outletService.getOrCreateOutlet(outletName))
                .thenReturn(outletId);


        when(entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, 1)))
                .thenReturn(dsl.newResult(ENTRY));

        when(entryMapper.jsonFrom(any(EntryRecord.class)))
                .thenReturn(OBJECT_MAPPER.nullNode());

        LorebookMapper mapper = new LorebookMapper(
                outletService,
                entryService,
                entryMapper
        );

        NewLorebookOrder order = mapper.orderFrom(mapper.jsonFrom(record));

        Asserts.assertRecordEqualsPayloadMinusFields(
                record, order.entityPayload(),
                Set.of(
                        LOREBOOKS.ID,
                        LOREBOOKS.NEXT_ENTRY_ID,
                        LOREBOOKS.CREATED
                )
        );
    }
}