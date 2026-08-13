package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.lorebook.entry.EntryMapper;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletServiceTestFactory;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LorebookMapperTest {

    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OutletService outletService;

    @Mock
    private EntryMapper entryMapper;

    @Mock
    private EntityReaders readers;

    @Mock
    private EntityReader<EntryRecord> entryReader;

    private LorebookMapper mapper;

    @BeforeEach
    void setUp() {
        outletService = OutletServiceTestFactory.mockService();

        mapper = new LorebookMapper(
                OBJECT_MAPPER,
                entryMapper,
                readers,
                outletService
        );
    }

    @Test
    void testRoundtrip() {
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

        when(readers.entries())
                .thenReturn(entryReader);

        when(entryReader.getMatching(any(EntityDataPayload.class)))
                .thenReturn(dsl.newResult(ENTRY));

        ZipBuilder zipBuilder = mock(ZipBuilder.class);

        NewLorebookOrder order =
                mapper.orderFrom(
                        mapper.jsonRecordFrom(record, zipBuilder)
                );

        Asserts.assertRecordEqualsPayloadMinusFields(
                record,
                order.payload(),
                Set.of(
                        LOREBOOKS.ID,
                        LOREBOOKS.NEXT_ENTRY_ID,
                        LOREBOOKS.CREATED
                )
        );
    }
}