package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.github.chechelpo.frplm.core.entities.fields.DTOMapper.DATA_CONSTRUCTION_MODE.*;
import static io.github.chechelpo.frplm.core.entities.fields.DTOMapper.KEY_CONSTRUCTION_MODE.*;
import static io.github.chechelpo.frplm.jooq.generated.Tables.TEST_TABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EntityControllerTest {

    private TestService service;
    private DTOMapper<TestTableRecord> mapper;
    private TestController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = mock(TestService.class);
        mapper = mock(DTOMapper.class);

        controller = new TestController(service, mapper);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static TestTableRecord record(
            int firstId,
            int secondId,
            String name,
            Integer counter,
            String description
    ) {
        TestTableRecord record = new TestTableRecord();

        record.set(TEST_TABLE.FIRST_ID, firstId);
        record.set(TEST_TABLE.SECOND_ID, secondId);
        record.set(TEST_TABLE.NAME, name);
        record.set(TEST_TABLE.COUNTER, counter);
        record.set(TEST_TABLE.DESCRIPTION, description);

        return record;
    }

    private static EntityDTO dto(
            int firstId,
            int secondId,
            String name,
            Integer counter,
            String description
    ) {
        return new TestDTO(
                EntityConfigs.Types.TEST_ENTITY.getEntityType(),
                Map.of(
                        "first_id", firstId,
                        "second_id", secondId
                ),
                Map.of(
                        "name", name,
                        "counter", counter,
                        "description", description
                )
        );
    }

    @Test
    void wrapEntityDelegatesToMapper() {
        TestTableRecord source =
                record(1, 2, "entity", 7, "description");

        EntityDTO expected =
                dto(1, 2, "entity", 7, "description");

        when(mapper.wrapRecord(source)).thenReturn(expected);

        EntityDTO result = controller.wrapEntity(source);

        assertSame(expected, result);
        verify(mapper).wrapRecord(source);
    }

    @Test
    void wrapEntityPassesNullToMapper() {
        when(mapper.wrapRecord(null)).thenReturn(null);

        EntityDTO result = controller.wrapEntity(null);

        assertNull(result);
        verify(mapper).wrapRecord(null);
    }

    @Test
    void wrapEntitiesVarargsDelegatesToMapper() {
        TestTableRecord first =
                record(1, 1, "first", 0, "first description");

        TestTableRecord second =
                record(2, 2, "second", 1, "second description");

        EntityDTO[] expected = {
                dto(1, 1, "first", 0, "first description"),
                dto(2, 2, "second", 1, "second description")
        };

        when(mapper.wrapRecords(List.of(first, second)))
                .thenReturn(expected);

        EntityDTO[] result =
                controller.wrapEntities(first, second);

        assertSame(expected, result);
        verify(mapper).wrapRecords(List.of(first, second));
    }

    @Test
    void wrapEntitiesListDelegatesToMapper() {
        TestTableRecord first =
                record(1, 1, "first", 0, "first description");

        TestTableRecord second =
                record(2, 2, "second", 1, "second description");

        List<TestTableRecord> records = List.of(first, second);

        EntityDTO[] expected = {
                dto(1, 1, "first", 0, "first description"),
                dto(2, 2, "second", 1, "second description")
        };

        when(mapper.wrapRecords(records)).thenReturn(expected);

        EntityDTO[] result = controller.wrapList(records);

        assertSame(expected, result);
        verify(mapper).wrapRecords(records);
    }

    @Test
    void queryReturnsBadRequestWhenQueryIsNull() {
        ResponseEntity<EntityDTO[]> response =
                controller.query(null);

        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody());

        verifyNoInteractions(mapper);
        verify(service, never()).getAll();
        verify(service, never()).getMatching(any(EntityDataPayload.class));
    }

    @Test
    void queryWithoutParametersReturnsAllWrappedRecords() {
        @SuppressWarnings("unchecked")
        Result<TestTableRecord> records = mock(Result.class);

        EntityDTO[] expected = {
                dto(1, 2, "first", 0, "description")
        };

        when(service.getAll()).thenReturn(records);
        when(mapper.wrapRecords(records)).thenReturn(expected);

        ResponseEntity<EntityDTO[]> response =
                controller.query(Map.of());

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());

        verify(service).getAll();
        verify(mapper).wrapRecords(records);

        verify(mapper, never()).getDataFrom(any(), any());
        verify(service, never()).getMatching(any(EntityDataPayload.class));
    }

    @Test
    void queryWithParametersUsesQueryModeAndReturnsMatchingRecords() {
        Map<String, Object> queryParameters = Map.of(
                "first_id", "1",
                "name", "matching"
        );

        EntityDataPayload<TestTableRecord> queryPayload =
                EntityDataPayload.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.NAME, "matching")
                        .build();

        @SuppressWarnings("unchecked")
        Result<TestTableRecord> records = mock(Result.class);

        EntityDTO[] expected = {
                dto(1, 2, "matching", 0, "description")
        };

        when(mapper.getDataFrom(queryParameters, QUERY))
                .thenReturn(queryPayload);

        when(service.getMatching(queryPayload))
                .thenReturn(records);

        when(mapper.wrapRecords(records))
                .thenReturn(expected);

        ResponseEntity<EntityDTO[]> response =
                controller.query(queryParameters);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());

        verify(mapper).getDataFrom(queryParameters, QUERY);
        verify(service).getMatching(queryPayload);
        verify(mapper).wrapRecords(records);
        verify(service, never()).getAll();
    }

    @Test
    void getReturnsWrappedRecordWhenEntityExists() {
        Map<String, Object> keyParameters = Map.of(
                "first_id", "1",
                "second_id", "2"
        );

        EntityKey<TestTableRecord> key =
                EntityKey.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.SECOND_ID, 2)
                        .build();

        TestTableRecord found =
                record(1, 2, "found", 0, "description");

        EntityDTO expected =
                dto(1, 2, "found", 0, "description");

        when(mapper.getKeyFromDTO(keyParameters, FULL_KEY))
                .thenReturn(key);

        when(service.find(key)).thenReturn(
                EntityReader.RecordFindResult.found(key, found)
        );

        when(mapper.wrapRecord(found)).thenReturn(expected);

        ResponseEntity<EntityDTO> response =
                controller.get(keyParameters);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());

        verify(mapper).getKeyFromDTO(keyParameters, FULL_KEY);
        verify(service).find(key);
        verify(mapper).wrapRecord(found);
    }

    @Test
    void getThrowsEntityNotFoundWhenEntityDoesNotExist() {
        Map<String, Object> keyParameters = Map.of(
                "first_id", "1",
                "second_id", "2"
        );

        EntityKey<TestTableRecord> key =
                EntityKey.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.SECOND_ID, 2)
                        .build();

        when(mapper.getKeyFromDTO(keyParameters, FULL_KEY))
                .thenReturn(key);

        when(service.find(key)).thenReturn(
                EntityReader.RecordFindResult.notFound(key)
        );

        assertThrows(
                EntityNotFound.class,
                () -> controller.get(keyParameters)
        );

        verify(mapper).getKeyFromDTO(keyParameters, FULL_KEY);
        verify(service).find(key);
        verify(mapper, never()).wrapRecord(any());
    }

    @Test
    void patchDelegatesToMapperAndService() {
        Map<String, Object> keyParameters = Map.of(
                "first_id", "1",
                "second_id", "2"
        );

        Map<String, Object> patchParameters = Map.of(
                "description", "updated description"
        );

        EntityKey<TestTableRecord> key =
                EntityKey.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.SECOND_ID, 2)
                        .build();

        EntityDataPayload<TestTableRecord> payload =
                EntityDataPayload.of(
                        TEST_TABLE.DESCRIPTION,
                        "updated description"
                );

        EntityUpdater.UpdateResult.Success<TestTableRecord> success =
                new EntityUpdater.UpdateResult.Success<>(
                        key,
                        payload
                );

        when(mapper.getKeyFromDTO(keyParameters, FULL_KEY))
                .thenReturn(key);

        when(mapper.getDataFrom(patchParameters, UPDATE))
                .thenReturn(payload);

        when(service.update(key, payload))
                .thenReturn(success);

        ResponseEntity<Boolean> response =
                controller.patch(
                        keyParameters,
                        patchParameters
                );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody());

        verify(mapper).getKeyFromDTO(
                keyParameters,
                FULL_KEY
        );

        verify(mapper).getDataFrom(
                patchParameters,
                UPDATE
        );

        verify(service).update(key, payload);
    }

    @Test
    void patchPropagatesMapperFailureWithoutCallingService() {
        Map<String, Object> keyParameters =
                Map.of("first_id", "1");

        Map<String, Object> patchParameters =
                Map.of("unknown", "value");

        RuntimeException failure =
                new RuntimeException("Invalid payload");

        when(mapper.getKeyFromDTO(
                keyParameters,
                FULL_KEY
        )).thenReturn(
                EntityKey.of(
                        TEST_TABLE.FIRST_ID,
                        1
                )
        );

        when(mapper.getDataFrom(
                patchParameters,
                UPDATE
        )).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> controller.patch(
                        keyParameters,
                        patchParameters
                )
        );

        assertSame(failure, thrown);

        verify(service, never())
                .update(any(EntityKey.class), any());
    }

    @Test
    void createMergesKeyAndPayloadParameters() throws Exception {
        installRequestContext("/test/entity");

        Map<String, Object> initialKey = Map.of(
                "first_id", "1",
                "second_id", "2"
        );

        Map<String, Object> initialData = Map.of(
                "name", "created",
                "description", "created description"
        );

        EntityDataPayload<TestTableRecord> payload =
                EntityDataPayload.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.SECOND_ID, 2)
                        .set(TEST_TABLE.NAME, "created")
                        .set(
                                TEST_TABLE.DESCRIPTION,
                                "created description"
                        )
                        .build();

        TestTableRecord created = record(
                1,
                2,
                "created",
                0,
                "created description"
        );

        EntityDTO expected = dto(
                1,
                2,
                "created",
                0,
                "created description"
        );

        when(mapper.getDataFrom(
                argThat(parameters ->
                        parameters.size() == 4
                                && parameters.get("first_id")
                                .equals("1")
                                && parameters.get("second_id")
                                .equals("2")
                                && parameters.get("name")
                                .equals("created")
                                && parameters.get("description")
                                .equals("created description")
                ),
                eq(CREATE)
        )).thenReturn(payload);

        when(service.createAndGet(payload))
                .thenReturn(created);

        when(mapper.wrapRecord(created))
                .thenReturn(expected);

        ResponseEntity<EntityDTO> response =
                controller.create(
                        initialKey,
                        initialData
                );

        assertEquals(201, response.getStatusCode().value());
        assertSame(expected, response.getBody());

        URI location =
                response.getHeaders().getLocation();

        assertNotNull(location);
        assertTrue(
                location.toString().contains("first_id=1")
        );
        assertTrue(
                location.toString().contains("second_id=2")
        );

        verify(service).createAndGet(payload);
        verify(mapper).wrapRecord(created);
    }

    @Test
    void createWithNullBodyUsesOnlyKeyParameters() throws Exception {
        installRequestContext("/test/entity");

        Map<String, Object> initialKey = Map.of(
                "first_id", "1",
                "second_id", "2"
        );

        EntityDataPayload<TestTableRecord> payload =
                EntityDataPayload.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.SECOND_ID, 2)
                        .build();

        TestTableRecord created =
                record(
                        1,
                        2,
                        "generated",
                        0,
                        "generated description"
                );

        EntityDTO expected =
                dto(
                        1,
                        2,
                        "generated",
                        0,
                        "generated description"
                );

        when(mapper.getDataFrom(
                initialKey,
                CREATE
        )).thenReturn(payload);

        when(service.createAndGet(payload))
                .thenReturn(created);

        when(mapper.wrapRecord(created))
                .thenReturn(expected);

        ResponseEntity<EntityDTO> response =
                controller.create(
                        initialKey,
                        null
                );

        assertEquals(201, response.getStatusCode().value());
        assertSame(expected, response.getBody());

        verify(mapper).getDataFrom(
                initialKey,
                CREATE
        );

        verify(service).createAndGet(payload);
    }

    @Test
    void createReturnsOkWhenLocationGenerationFails()
            throws Exception {

        /*
         * No request context is installed.
         * ServletUriComponentsBuilder throws,
         * and the controller intentionally falls back
         * to HTTP 200 with the DTO.
         */

        Map<String, Object> initialKey =
                Map.of("first_id", "1");

        EntityDataPayload<TestTableRecord> payload =
                EntityDataPayload.of(
                        TEST_TABLE.FIRST_ID,
                        1
                );

        TestTableRecord created =
                record(
                        1,
                        2,
                        "created",
                        0,
                        "description"
                );

        EntityDTO expected =
                dto(
                        1,
                        2,
                        "created",
                        0,
                        "description"
                );

        when(mapper.getDataFrom(
                initialKey,
                CREATE
        )).thenReturn(payload);

        when(service.createAndGet(payload))
                .thenReturn(created);

        when(mapper.wrapRecord(created))
                .thenReturn(expected);

        ResponseEntity<EntityDTO> response =
                controller.create(
                        initialKey,
                        null
                );

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        assertNull(
                response.getHeaders().getLocation()
        );
    }

    @Test
    void createPropagatesMapperFailureWithoutCallingService() {
        Map<String, Object> initialKey =
                Map.of("first_id", "1");

        Map<String, Object> initialData =
                Map.of("unknown", "value");

        RuntimeException failure =
                new RuntimeException(
                        "Invalid creation payload"
                );

        when(mapper.getDataFrom(
                any(),
                eq(CREATE)
        )).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> controller.create(
                        initialKey,
                        initialData
                )
        );

        assertSame(failure, thrown);

        verify(service, never())
                .createAndGet(any());
    }

    @Test
    void deleteDelegatesToMapperAndService() {
        Map<String, Object> keyParameters = Map.of(
                "first_id", "1",
                "second_id", "2"
        );

        EntityKey<TestTableRecord> key =
                EntityKey.<TestTableRecord>builder()
                        .set(TEST_TABLE.FIRST_ID, 1)
                        .set(TEST_TABLE.SECOND_ID, 2)
                        .build();

        when(mapper.getKeyFromDTO(
                keyParameters,
                FULL_KEY
        )).thenReturn(key);

        when(service.delete(key))
                .thenReturn(true);

        ResponseEntity<Boolean> response =
                controller.delete(keyParameters);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody());

        verify(mapper).getKeyFromDTO(
                keyParameters,
                FULL_KEY
        );

        verify(service).delete(key);
    }

    @Test
    void deleteReturnsFalseWhenServiceDoesNotDeleteRecord() {
        Map<String, Object> keyParameters =
                Map.of("first_id", "99");

        EntityKey<TestTableRecord> key =
                EntityKey.of(
                        TEST_TABLE.FIRST_ID,
                        99
                );

        when(mapper.getKeyFromDTO(
                keyParameters,
                FULL_KEY
        )).thenReturn(key);

        when(service.delete(key))
                .thenReturn(false);

        ResponseEntity<Boolean> response =
                controller.delete(keyParameters);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.FALSE, response.getBody());

        verify(mapper).getKeyFromDTO(
                keyParameters,
                FULL_KEY
        );

        verify(service).delete(key);
    }

    private static void installRequestContext(
            String requestUri
    ) {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI(requestUri);

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
    }

    private record TestDTO(
            String type,
            Map<String, Object> key,
            Map<String, Object> payload
    ) implements EntityDTO {
    }

    private static final class TestController
            extends EntityController<
            TestTableRecord,
            TestService
            > {

        private TestController(
                TestService service,
                DTOMapper<TestTableRecord> mapper
        ) {
            super(EntityConfigs.Types.TEST_ENTITY, service, mapper);
        }

        private EntityDTO[] wrapList(
                List<TestTableRecord> records
        ) {
            return wrapEntities(records);
        }
    }
}