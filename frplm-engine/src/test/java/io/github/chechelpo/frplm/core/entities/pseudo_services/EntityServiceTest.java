package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UneditableField;
import chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static chechelpo.frplm.jooq.generated.Tables.TEST_TABLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class EntityServiceTest {
    private static TestStore testStore;
    private static TestService testService;
    private static TestFields testFields;
    private static DSLContext dslContext;
    private static Connection connection;

    @BeforeAll
    public static void setUp() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        connection = TestDsl.newConnection();
        dslContext = TestDsl.newContext(connection);
        when(eventBus.nextOperationID()).thenReturn(1L, 2L, 3L, 4L, 5L);

        testStore = new TestStore(dslContext);
        testService = new TestService(testStore, eventBus);
        testFields = new TestFields(testService);
        testFields.register_field(
                TEST_TABLE.FIRST_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );
        testFields.register_field(
                TEST_TABLE.SECOND_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );
        testFields.register_field(
                TEST_TABLE.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                        )
                        .build()
        );
        testFields.register_field(
                TEST_TABLE.COUNTER,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
        testFields.register_field(
                TEST_TABLE.DESCRIPTION,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .nullable()
                                .setMinLength(20)
                        )
                        .build()
        );
    }
    @BeforeEach
    public void beforeEach() {
        TestDsl.dropSchema(dslContext);
        TestDsl.createSchema(dslContext);
    }
    @AfterAll
    public static void tearDown() throws Exception {
        TestDsl.dropSchema(dslContext);
        connection.close();
    }

    @Test
    void isKey() {
        assertTrue(testService.isKey(TEST_TABLE.FIRST_ID), "Test service first id is not key");
        assertTrue(testService.isKey(TEST_TABLE.SECOND_ID), "Test service second id is not key");
    }
    @Test
    void keyOf() {
        int firstId = 0;
        int secondId = 0;
        EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, firstId)
                .set(TEST_TABLE.SECOND_ID, secondId)
                .set(TEST_TABLE.NAME, "test_try")
                .build();

        assertDoesNotThrow(() -> testService.createAndGet(data));
        EntityKey<TestTableRecord> key = EntityKey.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, firstId)
                .set(TEST_TABLE.SECOND_ID, secondId)
                .build();
        Optional<TestTableRecord> record = testService.find(key);
        assertTrue(record.isPresent());
        assertEquals(key, testService.keyOf(record.get()), "Mismatch in expected constructed key");
    }
    
    @Test
    public void testIncrementAndGet(){
        int firstId = 1;
        int secondId = 2;
        EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, firstId)
                .set(TEST_TABLE.SECOND_ID, secondId)
                .set(TEST_TABLE.NAME, "test_try")
                .build();
        assertDoesNotThrow(() -> testService.createAndGet(data));
        EntityKey<TestTableRecord> key = EntityKey.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, firstId)
                .set(TEST_TABLE.SECOND_ID, secondId)
                .build();
        int startCounter = 0;
        Optional<Integer> supposedStartCounter = testService.getValueOf(TEST_TABLE.COUNTER, key);
        assertTrue(supposedStartCounter.isPresent());
        assertEquals(startCounter, supposedStartCounter.get(),
                "Mismatch in expected default state of TestTable.counter");

        Optional<Integer> nextCounter = testService.incrementAndGet(TEST_TABLE.COUNTER, key);
        assertTrue(nextCounter.isPresent(), "Could not increment nextCounter");
        assertEquals(startCounter + 1, nextCounter.get(),
                "Mismatch in expected state of TestTable.counter after increment and get");

        Optional<Integer> persistedCounter = testService.getValueOf(TEST_TABLE.COUNTER, key);
        assertTrue(persistedCounter.isPresent());
        assertEquals(startCounter + 1, persistedCounter.get(),
                "Counter was not persisted after increment");

        EntityKey<TestTableRecord> unknownKey = EntityKey.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, 1020)
                .set(TEST_TABLE.SECOND_ID, 12)
                .build();
        assertTrue(testService.getValueOf(TEST_TABLE.COUNTER, unknownKey).isEmpty(),
                "Could increment value of unregistered entity");
        assertTrue(testService.find(unknownKey).isEmpty(),
                "Created phantom record");
    }
    @Test
    public void emptyOptionalOnUnknownKey(){
        EntityKey<TestTableRecord> unknownKey = EntityKey.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, 1020)
                .set(TEST_TABLE.SECOND_ID, 12)
                .build();
        assertTrue(testService.find(unknownKey).isEmpty(), "Created phantom record");
    }
    @Test
    public void getMatchingWithPartialKeyReturnsExpectedRecords() {
        int matchingFirstId = 10;
        int otherFirstId = 20;

        int matchingAmount = 7;
        int nonMatchingAmount = 5;

        List<Integer> expectedSecondIds = new ArrayList<>(matchingAmount);

        for (int i = 0; i < matchingAmount; i++) {
            int secondId = 100 + i;
            expectedSecondIds.add(secondId);

            EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                    .set(TEST_TABLE.FIRST_ID, matchingFirstId)
                    .set(TEST_TABLE.SECOND_ID, secondId)
                    .set(TEST_TABLE.NAME, "matching_" + i)
                    .build();

            assertDoesNotThrow(() -> testService.createAndGet(data));
        }

        for (int i = 0; i < nonMatchingAmount; i++) {
            EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                    .set(TEST_TABLE.FIRST_ID, otherFirstId)
                    .set(TEST_TABLE.SECOND_ID, 200 + i)
                    .set(TEST_TABLE.NAME, "non_matching_" + i)
                    .build();

            assertDoesNotThrow(() -> testService.createAndGet(data));
        }

        EntityKey<TestTableRecord> partialKey = EntityKey.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, matchingFirstId)
                .build();

        List<TestTableRecord> result = testService.getMatching(partialKey);

        assertEquals(matchingAmount, result.size(), "Wrong number of matching records");

        assertTrue(
                result.stream().allMatch(record -> record.getFirstId().equals(matchingFirstId)),
                "Found record with unexpected FIRST_ID"
        );

        Set<Integer> actualSecondIds = result.stream()
                .map(TestTableRecord::getSecondId)
                .collect(Collectors.toSet());

        assertEquals(
                Set.copyOf(expectedSecondIds),
                actualSecondIds,
                "Returned records do not match expected SECOND_ID set"
        );
    }
    @Test
    public void tryUpdateOnKey(){
        int firstId = 1;
        int secondId = 2;
        EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, firstId)
                .set(TEST_TABLE.SECOND_ID, secondId)
                .set(TEST_TABLE.NAME, "test_try")
                .build();
        assertDoesNotThrow(() -> testService.createAndGet(data));
        EntityKey<TestTableRecord> key = EntityKey.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, firstId)
                .set(TEST_TABLE.SECOND_ID, secondId)
                .build();
        Optional<TestTableRecord> returnValue = testService.find(key);
        assertTrue(returnValue.isPresent());
        assertThrows(UneditableField.class, () -> testService.update(key,
                EntityDataPayload.of(TEST_TABLE.FIRST_ID, 200))
        );
        assertThrows(UneditableField.class, () -> testService.update(key,
                EntityDataPayload.of(TEST_TABLE.SECOND_ID, 300)
        ));
    }
    @Test
    public void testIncompleteFieldsOnCreate(){
        EntityDataPayload<TestTableRecord> data = EntityDataPayload.of(TEST_TABLE.FIRST_ID, 1);
        EntityDataPayload<TestTableRecord> data2 = EntityDataPayload.of(TEST_TABLE.SECOND_ID, 2);

        assertThrows(InvalidValue.class, () -> testService.createAndGet(data));
        assertThrows(InvalidValue.class, () -> testService.createAndGet(data2));
    }
    @Test
    public void testNonNullableField(){
        EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, 1)
                .set(TEST_TABLE.SECOND_ID, 2)
                .set(TEST_TABLE.NAME, null)
                .build();
        assertThrows(InvalidValue.class, () -> testService.createAndGet(data));
    }
    @Test
    public void nullableField(){
        EntityDataPayload<TestTableRecord> data = EntityDataPayload.<TestTableRecord>builder()
                .set(TEST_TABLE.FIRST_ID, 1)
                .set(TEST_TABLE.SECOND_ID, 2)
                .set(TEST_TABLE.NAME, " ")
                .set(TEST_TABLE.DESCRIPTION, null)
                .build();
        assertDoesNotThrow(() -> testService.createAndGet(data));
    }
    @Test
    public void testAllCorrectLifeCycle() {
        int firstIDcounter = 0;
        int secondIDcounter = 0;
        int testAmount = 100;
        long seed = 110000;
        List<EntityDataPayload<TestTableRecord>> data = new ArrayList<>(testAmount);
        for (int i = 0; i < testAmount; i++) {
            data.add(EntityDataPayload.<TestTableRecord>builder()
                    .set(TEST_TABLE.FIRST_ID, firstIDcounter)
                    .set(TEST_TABLE.SECOND_ID, secondIDcounter)
                    .set(TEST_TABLE.NAME, TestText.randomText(seed + (2L * i), 2, 255))
                    .set(TEST_TABLE.DESCRIPTION, TestText.randomText(seed + (2L * i) + 1, 20, 2000))
                    .build()
            );
            firstIDcounter++;
            secondIDcounter++;
        }

        List<EntityKey<TestTableRecord>> keys = data.stream()
                .map(payload -> testService.keyOf(testService.createAndGet(payload)))
                .toList();
        
        assertEquals(keys.size(), data.size(), "Wrong number of keys or data");
        assertEquals(testAmount, keys.size());
        //Assert values remain unchanged
        for (int i = 0 ; i < testAmount ; i++) {
            EntityKey<TestTableRecord> key = keys.get(i);
            assertTrue(testService.exists(key), "Key " + key + " not found after create");
            Optional<TestTableRecord> findResult = testService.find(key);
            assertTrue(findResult.isPresent(), "Could not find record of key " + key);

            TestTableRecord found = findResult.get();
            EntityDataPayload<TestTableRecord> payload = data.get(i);
            assertEquals(found.getName(), payload.requireValue(TEST_TABLE.NAME), "Name is not equal on record " + i);
            assertEquals(found.getDescription(), payload.requireValue(TEST_TABLE.DESCRIPTION), "Description is not equal on record " + i);
        }

        //Assert updates  remain unchanged
        for (int i = 0; i < testAmount ; i++) {
            EntityKey<TestTableRecord> key = keys.get(i);
            EntityDataPayload<TestTableRecord> newValue = EntityDataPayload
                    .of(TEST_TABLE.DESCRIPTION, TestText.randomText(testAmount + 100 - i, 25, 2000));
            assertTrue(testService.update(key, newValue), "Update failed for key: " + key);

            Optional<String> possibleValue = testService.getValueOf(TEST_TABLE.DESCRIPTION, key);
            assertTrue(possibleValue.isPresent(), "Could not find value for key " + key);
            assertEquals(newValue.requireValue(TEST_TABLE.DESCRIPTION), possibleValue.get(), "Mismatch after update");
        }
        //Delete without issues
        keys.forEach(entityKey -> {
            assertTrue(testService.delete(entityKey), "Couldn't delete key " + entityKey);
            assertFalse(testService.delete(entityKey), "Deleted key twice " + entityKey);
            assertFalse(testService.exists(entityKey), "Key still exists " + entityKey);
            assertTrue(testService.find(entityKey).isEmpty(), "Found deleted record " + entityKey);
        });
        assertEquals(0, testService.getAll().size(), "Rows remained after deleting all records");
    }

}