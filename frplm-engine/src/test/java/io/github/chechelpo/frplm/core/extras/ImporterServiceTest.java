package io.github.chechelpo.frplm.core.extras;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.utils.json_mappers.LorebookMapper;
import io.github.chechelpo.frplm.utils.json_mappers.WorldMapper;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewEntryOrder;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewLorebookOrder;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImporterServiceLorebookTest {

    @Mock LorebookMapper lorebookMapper;
    @Mock LorebookService lorebookService;
    @Mock EntryService entryService;
    @Mock EntryKeywordService entryKeywordService;
    @Mock WorldMapper worldMapper;
    @Mock WorldService worldService;
    @Mock LocationsService locationsService;
    @Mock EdgeService edgeService;
    @Mock CharacterService characterService;
    @Mock StartingLocationsService startingLocationsService;
    @Mock RegionService regionService;

    @Captor ArgumentCaptor<EntityDataPayload<LorebooksRecord>> payloadCaptor;

    ImporterService importerService
    @BeforeEach
    void setUp(){
        importerService = new ImporterService(
                lorebookMapper, lorebookService, entryService, entryKeywordService,
                worldMapper, worldService, locationsService, edgeService,
                characterService, startingLocationsService, regionService
        );
    }

    @Test
    void executeLorebook_delegatesToLorebookService_andRunsEntries() {
        // Stub the service to return a fake record
        LorebooksRecord fakeRecord = new LorebooksRecord(42, null, "My Lorebook", null, 1);
        when(lorebookService.createAndGet(any())).thenReturn(fakeRecord);

        EntityDataPayload<LorebooksRecord> payload = EntityDataPayload.<LorebooksRecord>builder().build();
        NewEntryOrder entryOrder = mock(NewEntryOrder.class);
        when(entryOrder.keywords()).thenReturn(Set.of());
        NewLorebookOrder order = new NewLorebookOrder(payload, List.of(entryOrder));

        LorebooksRecord result = importerService.executeLorebook(order);

        assertSame(fakeRecord, result);

        // Verify the call captured the exact payload passed in
        verify(lorebookService).createAndGet(payloadCaptor.capture());
        assertSame(payload, payloadCaptor.getValue(),
                "createAndGet should be called with order.entityPayload()");

        // Verify entries were executed against the right lorebookId
        verify(entryService).createAndGet(any());
        verifyNoMoreInteractions(lorebookService, entryService, entryKeywordService);
    }
}