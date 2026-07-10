package io.github.chechelpo.frplm.core.extras;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.FullDomainContext;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.utils.json_mappers.*;
import io.github.chechelpo.frplm.utils.json_mappers.orders.*;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.world.region.RegionTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import chechelpo.frplm.jooq.generated.tables.records.*;
import org.jooq.impl.UpdatableRecordImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.*;

import static chechelpo.frplm.jooq.generated.Tables.*;
import static chechelpo.frplm.jooq.generated.tables.Entry.ENTRY;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(FullDomainContext.class)
class ImporterServiceTest {
    @Autowired
    ImporterService importerService;
    @Autowired
    FullDomainContext domain;
    @Autowired
    private EntryMapper entryMapper;
    @Autowired
    private LorebookMapper lorebookMapper;
    @Autowired
    private LorebookService lorebookService;
    @Autowired
    private CharacterMapper characterMapper;
    @Autowired
    private RegionMapper regionMapper;
    @Autowired
    private LocationMapper locationMapper;

    @BeforeEach
    void setUp() {
        domain.reload();
    }

    @Test
    void importWorld_roundtrip() {
        throw new UnsupportedOperationException("Test not implemented");
    }

    @Test
    void executeLorebook_roundtrip() {
        Map<LorebooksRecord, List<EntryRecord>> generated = domain.entries.createEntries(10L, 1, 100);

        List<EntryRecord> expectedEntries = generated.values().stream().toList().getFirst();
        LorebooksRecord expectedLorebook = generated.keySet().stream().toList().getFirst();
        LorebookService lorebookService = domain.entries.lorebooks.service;
        String expectedOutletName = domain.entries.lorebooks.outlets.outletService.getOutletName(expectedLorebook.getDefaultOutletId())
                .orElseThrow();

        NewLorebookOrder lorebookOrder = lorebookMapper.orderFrom(lorebookMapper.jsonFrom(expectedLorebook));
        assertTrue(lorebookService.delete(lorebookService.keyOf(expectedLorebook)), "Could not delete lorebook");

        assertDoesNotThrow(() -> importerService.executeLorebook(lorebookOrder), "Could not execute lorebook order");

        List<LorebooksRecord> allLorebooks = lorebookService.getAll();
        assertEquals(1, allLorebooks.size(), "Expected one lorebook for this test");
        LorebooksRecord actualLorebook = allLorebooks.getFirst();

        assertEquals(expectedLorebook.getName(), actualLorebook.getName(), "Name mismatch");

        Optional<String> actualOutlet = domain.entries.lorebooks.outlets.outletService.getOutletName(actualLorebook.getDefaultOutletId());
        assertTrue(actualOutlet.isPresent(), "Did not create outlet" + expectedOutletName);
        assertEquals(expectedOutletName, actualOutlet.get(), "Default outlet mismatch");

        List<EntryRecord> actualEntries = domain.entries.entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, actualLorebook.getId()));
        assertEquals(expectedEntries.size(), actualEntries.size(), "Mismatch in created entries size");
        for (int i = 0; i < actualEntries.size(); i++)
            assertEntryEqualsIgnoringKeys(expectedEntries.get(i), actualEntries.get(i));
    }

    private static void assertLorebookEqualsIgnoringKeys(
            LorebooksRecord expected,
            List<EntryRecord> expectedEntries,
            LorebooksRecord actual,
            List<EntryRecord> actualEntries
    ) {
        assertEquals(expected.getName(), actual.getName(), "Name mismatch");
        assertEquals(expectedEntries.size(), actualEntries.size(), "Mismatch in created entries size");
        for (int i = 0; i < actualEntries.size(); i++)
            assertEntryEqualsIgnoringKeys(expectedEntries.get(i), actualEntries.get(i));
    }

    @Test
    void executeEntry_Roundtrip() {
        Set<String> keywords = Set.of("one", "two", "three");
        Map<LorebooksRecord, List<EntryRecord>> generated = domain.entries.createEntries(10L, 1, 1);
        EntryService entryService = domain.entries.entryService;

        EntryRecord expectedEntry = generated.values().stream().toList().getFirst().getFirst();
        LorebooksRecord lorebook = generated.keySet().stream().toList().getFirst();
        keywords.forEach(key ->
                domain.entries.entryKeywords.entryKeywordsService.associate(expectedEntry.getLorebookId(), expectedEntry.getEntryId(), key)
        );

        NewEntryOrder order = entryMapper.orderFrom(entryMapper.jsonFrom(expectedEntry));
        assertTrue(entryService.delete(entryService.keyOf(expectedEntry)), "Could not delete entry");
        assertDoesNotThrow(
                () -> importerService.executeEntry(order, lorebook.getId()),
                "Could not execute order"
        );

        List<EntryRecord> actualEntries = entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, lorebook.getId()));
        assertEquals(1, actualEntries.size());
        EntryRecord actualEntry = actualEntries.getFirst();

        assertEntryEqualsIgnoringKeys(expectedEntry, actualEntry);
        assertEquals(
                domain.entries.entryKeywords.entryKeywordsService.keywordsOfEntry(actualEntry.getLorebookId(), actualEntry.getEntryId()),
                keywords,
                "Mismatch in keywords"
        );
    }

    private static void assertEntryEqualsIgnoringKeys(
            EntryRecord expected,
            EntryRecord actual
    ) {
        assertAll(
                java.util.Arrays.stream(ENTRY.fields())
                        .filter(field -> !field.equals(ENTRY.ENTRY_ID))
                        .filter(field -> !field.equals(ENTRY.LOREBOOK_ID))
                        .map(field -> () -> assertEquals(
                                expected.get(field),
                                actual.get(field),
                                "Mismatch in field: " + field.getName()
                        ))
        );
    }

    @Test
    void executeCharacter_Roundtrip() {
        CharacterCoreTestContext characterContext = domain.sessions.characters;

        CharactersRecord expectedCharacter = characterContext.createAndGetRecords(1).getFirst();
        LorebooksRecord expectedLorebook = domain.entries.lorebooks.service.getLorebookOf(expectedCharacter);
        List<EntryRecord> expectedEntries = domain.entries.entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, expectedLorebook.getId()));

        NewCharacterOrder characterOrder = characterMapper.fromJson(characterMapper.jsonFrom(expectedCharacter));
        assertTrue(characterContext.service.delete(characterContext.service.keyOf(expectedCharacter)), "Can't delete character");

        assertDoesNotThrow(
                () -> importerService.executeCharacter(characterOrder),
                "Could not execute character"
        );
        List<CharactersRecord> actualCharacter = characterContext.service.getAll();
        assertEquals(1, actualCharacter.size(), "More characters than expected");
        LorebooksRecord actualLorebook = domain.entries.lorebooks.service.getLorebookOf(actualCharacter.getFirst());
        List<EntryRecord> actualEntries = domain.entries.entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, actualLorebook.getId()));

        assertCharacterEqualsIgnoringKeys(
                expectedCharacter,
                actualCharacter.getFirst()
        );

        assertLorebookEqualsIgnoringKeys(
                expectedLorebook,
                expectedEntries,
                actualLorebook,
                actualEntries
        );
    }

    private static void assertCharacterEqualsIgnoringKeys(
            CharactersRecord expected,
            CharactersRecord actual
    ) {
        assertAll(
                java.util.Arrays.stream(CHARACTERS.fields())
                        .filter(field -> !field.equals(CHARACTERS.ID))
                        .filter(field -> !field.equals(CHARACTERS.LOREBOOK_ID))
                        .filter(field -> !field.equals(CHARACTERS.CREATED))
                        .map(field -> () -> assertEquals(
                                expected.get(field),
                                actual.get(field),
                                "Mismatch in field: " + field.getName()
                        ))
        );
    }

    @Test
    void executeRegions_Roundtrip() {
        RegionTestContext regionContext = domain.regions;
        List<RegionRecord> expectedRegions = regionContext.createRegions(100, null);
        regionContext.linkRegionsAsAcyclicChains(expectedRegions);
        expectedRegions = regionContext.service.getAll();
        expectedRegions.forEach(UpdatableRecordImpl::refresh);
        int worldId = expectedRegions.getFirst().getWorldId();

        Map<RegionRecord, String> expectedParentName = new HashMap<>(expectedRegions.size());
        List<NewRegionOrder> orders = new ArrayList<>(expectedRegions.stream()
                .map(reg -> {
                    NewRegionOrder order = regionMapper.fromJson(regionMapper.toJson(reg));
                    if (order.parentRegionName() != null)
                        expectedParentName.put(reg, order.parentRegionName());
                    return order;
                })
                .toList());

        Queue<RegionRecord> regionsToDelete = new ArrayDeque<>(expectedRegions);
        while (!regionsToDelete.isEmpty()) {
            RegionRecord tryDelete = regionsToDelete.poll();
            try {
                EntityKey<RegionRecord> key = regionContext.service.keyOf(tryDelete);
                if (!regionContext.service.exists(key)) continue;
                regionContext.service.delete(key);
            } catch (UnsupportedAction ignored) {
                regionsToDelete.add(tryDelete);
            }
        }
        assertEquals(0, regionContext.service.getAll().size(), "Could not delete all records");

        List<RegionRecord> finalExpectedRegions = expectedRegions;
        assertDoesNotThrow(
                () -> importerService.executeRegions(orders, finalExpectedRegions.getFirst().getWorldId()),
                "Execute regions could nod be completed"
        );
        List<RegionRecord> actualRegions = domain.regions.service.getAll();

        assertEquals(expectedRegions.size(), actualRegions.size(), "Mismatch in regions created size");
        for (int i = 0; i < expectedRegions.size(); i++) {
            RegionRecord expectedRegion = expectedRegions.get(i);
            RegionRecord actualRegion = actualRegions.get(i);

            assertRegionEquals(expectedRegion, actualRegion);
            if (expectedRegion.getParentRegionId() != null) {
                assertNotNull(actualRegion.getParentRegionId(), "Actual has no parent");
                EntityKey<RegionRecord> parentKey = EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worldId)
                        .set(REGION.ID, actualRegion.getParentRegionId())
                        .build();
                Optional<RegionRecord> actualParent = regionContext.service.find(parentKey);
                assertTrue(actualParent.isPresent(), "Couldn't find parent");
                assertEquals(
                        expectedParentName.get(expectedRegion),
                        actualParent.get().getName(),
                        "Mismatch in parent name"
                );
            }
        }
    }

    private static void assertRegionEquals(
            RegionRecord expected,
            RegionRecord actual
    ) {
        assertAll(
                java.util.Arrays.stream(REGION.fields())
                        .filter(field -> !field.equals(REGION.ID))
                        .filter(field -> !field.equals(REGION.PARENT_REGION_ID))
                        .filter(field -> !field.equals(REGION.LOREBOOK_ID))
                        .map(field -> () -> assertEquals(
                                expected.get(field),
                                actual.get(field),
                                "Mismatch in field: " + field.getName()
                        ))
        );
    }

    @Test
    void executeLocations_RoundtripNoStartingLocations() {
        int locationsPerRegion = 10;
        List<RegionRecord> regions = domain.regions.createRegions(100, locationsPerRegion);
        int worldId = regions.getFirst().getWorldId();
        Map<RegionRecord, List<LocationsRecord>> expectedPerRegion = new HashMap<>(regions.size());
        regions.forEach(region ->
                expectedPerRegion.put(
                        region,
                        domain.locations.service.getLocationsOfRegion(worldId, region.getId())
                )
        );

        List<NewLocationOrder> orders = expectedPerRegion.values().stream().flatMap(List::stream)
                .map(loc -> locationMapper.orderFrom(locationMapper.jsonFrom(loc)))
                .toList();
        expectedPerRegion.values().stream().flatMap(List::stream)
                .forEach(loc ->
                        assertTrue(domain.locations.service.delete(domain.locations.service.keyOf(loc)))
                );

        Map<String, RegionRecord> regionsByName = new HashMap<>(expectedPerRegion.size());
        regions.forEach(reg -> regionsByName.put(reg.getName(), reg));
        assertDoesNotThrow(
                () -> importerService.executeLocationsAndCharacters(orders, regionsByName, worldId)
        );

        for (RegionRecord region : regions){
            List<LocationsRecord> actualLocations = domain.locations.service.getLocationsOfRegion(worldId, region.getId());
            List<LocationsRecord> expectedLocations = expectedPerRegion.get(region);

            assertEquals(expectedLocations.size(), actualLocations.size(), "Mismatch in locations sizes");
            actualLocations.forEach( actualLocation ->
                    {
                        Optional<LocationsRecord> expectedLocation = expectedLocations.stream()
                                .filter(other -> actualLocation.getName().equals(other.getName()))
                                .findFirst();
                        assertTrue(expectedLocation.isPresent(), "Could not find matching location");
                        assertLocationEquals(actualLocation, expectedLocation.get());
                    }
            );
        }
    }
    private void assertLocationEquals(
            LocationsRecord actual,
            LocationsRecord expected
    ){
        assertAll(
                java.util.Arrays.stream(LOCATIONS.fields())
                        .filter(field -> !field.equals(LOCATIONS.ID))
                        .filter(field -> !field.equals(LOCATIONS.LOREBOOK_ID))
                        .map(field -> () -> assertEquals(
                                expected.get(field),
                                actual.get(field),
                                "Mismatch in field: " + field.getName()
                        ))
        );
    }
    
}