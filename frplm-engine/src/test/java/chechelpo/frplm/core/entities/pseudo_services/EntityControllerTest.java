package chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.InvalidKey;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.jooq.generated.tables.records.TestTableRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.TEST_TABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntityControllerTest {
    private TestService service;
    private TestController controller;
    private TestControllerFields fields;
    enum DTOFields{
        FirstID("first_id"),
        SecondID("second_id"),
        Name("name"),
        Description("description"),
        Counter("counter")
        ;
        private final String value;
        DTOFields(String value){
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @BeforeEach
    void setUp() {
        service = mock(TestService.class);

        when(service.getType()).thenReturn(EntityTypes.Types.TEST_ENTITY);
        when(service.isKey(TEST_TABLE.FIRST_ID)).thenReturn(true);
        when(service.isKey(TEST_TABLE.SECOND_ID)).thenReturn(true);
        when(service.isKey(TEST_TABLE.NAME)).thenReturn(false);
        when(service.isKey(TEST_TABLE.DESCRIPTION)).thenReturn(false);
        when(service.isKey(TEST_TABLE.COUNTER)).thenReturn(false);

        controller = new TestController(service);
        fields = new TestControllerFields(service,controller);


        fields.register_field(
                DTOFields.FirstID.toString(),
                TEST_TABLE.FIRST_ID,
                FieldInfo.numberField(FieldType.INTEGER).build()
        );

        fields.register_field(
                DTOFields.SecondID.toString(),
                TEST_TABLE.SECOND_ID,
                FieldInfo.numberField(FieldType.INTEGER).build()
        );

        fields.register_field(
                DTOFields.Name.toString(),
                TEST_TABLE.NAME,
                FieldInfo.stringField().build()
        );

        fields.register_field(
                DTOFields.Description.toString(),
                TEST_TABLE.DESCRIPTION,
                FieldInfo.stringField().build()
        );

        fields.register_field(
                DTOFields.Counter.toString(),
                TEST_TABLE.COUNTER,
                FieldInfo.numberField(FieldType.INTEGER).build()
        );
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

    @Test
    void registerPublicFieldRejectsDuplicateDtoName() {
        assertThrows(IllegalStateException.class, () ->
                controller.registerPublicField(
                        TEST_TABLE.COUNTER,
                        DTOFields.Name.toString(),
                        FieldInfo.numberField(FieldType.INTEGER).build().format
                )
        );
    }

    @Test
    void registerPublicFieldIgnoresNullDtoName() {
        assertDoesNotThrow(() ->
                controller.registerPublicField(
                        TEST_TABLE.NAME,
                        null,
                        FieldInfo.stringField().build().format
                )
        );
    }

    @Test
    void extractKeyCoercesRegisteredExternalNames() {
        EntityKey<TestTableRecord> key = controller.extractKey(Map.of(
                DTOFields.FirstID.toString(), "1",
                DTOFields.SecondID.toString(), "2"
        ));

        assertEquals(1, key.getValue(TEST_TABLE.FIRST_ID));
        assertEquals(2, key.getValue(TEST_TABLE.SECOND_ID));
    }

    @Test
    void extractKeyThrowsInvalidKeyWhenExternalNameIsUnknown() {
        assertThrows(InvalidKey.class, () ->
                controller.extractKey(Map.of("unknown", "1"))
        );
    }

    @Test
    void extractPayloadCoercesRegisteredExternalNames() {
        EntityDataPayload<TestTableRecord> payload = controller.extractPayload(Map.of(
                DTOFields.FirstID.toString(), "1",
                DTOFields.SecondID.toString(), "2",
                DTOFields.Name.toString(), "created",
                DTOFields.Description.toString(), "created description text",
                DTOFields.Counter.toString(), "10"
        ));

        assertEquals(1, payload.requireValue(TEST_TABLE.FIRST_ID));
        assertEquals(2, payload.requireValue(TEST_TABLE.SECOND_ID));
        assertEquals("created", payload.requireValue(TEST_TABLE.NAME));
        assertEquals("created description text", payload.requireValue(TEST_TABLE.DESCRIPTION));
        assertEquals(10, payload.requireValue(TEST_TABLE.COUNTER));
    }

    @Test
    void extractPayloadThrowsInvalidValueWhenExternalNameIsUnknown() {
        assertThrows(InvalidValue.class, () ->
                controller.extractPayload(Map.of("unknown", "value"))
        );
    }

    @Test
    void wrapEntityReturnsNullWhenRecordIsNull() {
        assertNull(controller.wrapEntity(null));
    }

    @Test
    void wrapEntitySplitsKeyAndPayloadFields() {
        TestTableRecord record = record(
                1,
                2,
                "entity-name",
                7,
                "entity description text"
        );

        EntityController.EntityDTO dto = controller.wrapEntity(record);

        assertNotNull(dto);
        assertEquals(EntityTypes.Types.TEST_ENTITY.getEntityType(), dto.type());

        assertEquals(2, dto.key().size());
        assertEquals(1, dto.key().get(DTOFields.FirstID.toString()));
        assertEquals(2, dto.key().get(DTOFields.SecondID.toString()));

        assertEquals(3, dto.payload().size());
        assertEquals("entity-name", dto.payload().get(DTOFields.Name.toString()));
        assertEquals(7, dto.payload().get(DTOFields.Counter.toString()));
        assertEquals("entity description text", dto.payload().get(DTOFields.Description.toString()));
    }

    @Test
    void wrapEntitiesVarargsWrapsAllRecords() {
        TestTableRecord first = record(1, 1, "first", 0, "first description text");
        TestTableRecord second = record(2, 2, "second", 1, "second description text");

        EntityController.EntityDTO[] dtos = controller.wrapEntities(first, second);

        assertEquals(2, dtos.length);
        assertEquals(1, dtos[0].key().get(DTOFields.FirstID.toString()));
        assertEquals(2, dtos[1].key().get(DTOFields.FirstID.toString()));
    }

    @Test
    void wrapEntitiesListWrapsAllRecords() {
        TestTableRecord first = record(1, 1, "first", 0, "first description text");
        TestTableRecord second = record(2, 2, "second", 1, "second description text");

        EntityController.EntityDTO[] dtos = controller.wrapEntities(List.of(first, second));

        assertEquals(2, dtos.length);
        assertEquals("first", dtos[0].payload().get(DTOFields.Name.toString()));
        assertEquals("second", dtos[1].payload().get(DTOFields.Name.toString()));
    }

    @Test
    void queryWithBodyDelegatesToGetMatchingAndReturnsWrappedDtos() {
        TestTableRecord matching = record(10, 100, "matching", 0, "matching description text");

        when(service.getMatching(any())).thenReturn(List.of(matching));

        ResponseEntity<EntityController.EntityDTO[]> response = controller.query(Map.of(
                DTOFields.FirstID.toString(), "10"
        ));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length);

        assertEquals(10, response.getBody()[0].key().get(DTOFields.FirstID.toString()));
        assertEquals(100, response.getBody()[0].key().get(DTOFields.SecondID.toString()));
        assertEquals("matching", response.getBody()[0].payload().get(DTOFields.Name.toString()));

        verify(service).getMatching(argThat(key ->
                key.getValue(TEST_TABLE.FIRST_ID).equals(10)
        ));
        verify(service, never()).getAll();
    }

    @Test
    void getReturnsWrappedEntityWhenServiceFindsRecord() {
        TestTableRecord found = record(1, 2, "found", 0, "found description text");

        when(service.find(any())).thenReturn(Optional.of(found));

        ResponseEntity<EntityController.EntityDTO> response = controller.get(Map.of(
                DTOFields.FirstID.toString(), "1",
                DTOFields.SecondID.toString(), "2"
        ));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(1, response.getBody().key().get(DTOFields.FirstID.toString()));
        assertEquals(2, response.getBody().key().get(DTOFields.SecondID.toString()));
        assertEquals("found", response.getBody().payload().get(DTOFields.Name.toString()));

        verify(service).find(argThat(key ->
                key.getValue(TEST_TABLE.FIRST_ID).equals(1)
                        && key.getValue(TEST_TABLE.SECOND_ID).equals(2)
        ));
    }

    @Test
    void getThrowsEntityNotFoundWhenServiceReturnsEmpty() {
        when(service.find(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFound.class, () ->
                controller.get(Map.of(
                        DTOFields.FirstID.toString(), "1",
                        DTOFields.SecondID.toString(), "2"
                ))
        );

        verify(service).find(argThat(key ->
                key.getValue(TEST_TABLE.FIRST_ID).equals(1)
                        && key.getValue(TEST_TABLE.SECOND_ID).equals(2)
        ));
    }

    @Test
    void patchDelegatesToServiceUpdateAndReturnsBooleanBody() {
        when(service.update(any(), any())).thenReturn(true);

        ResponseEntity<Boolean> response = controller.patch(
                Map.of(
                        DTOFields.FirstID.toString(), "1",
                        DTOFields.SecondID.toString(), "2"
                ),
                Map.of(DTOFields.Description.toString(), "updated description text")
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody());

        verify(service).update(
                argThat(key ->
                        key.getValue(TEST_TABLE.FIRST_ID).equals(1)
                                && key.getValue(TEST_TABLE.SECOND_ID).equals(2)
                ),
                argThat(payload ->
                        payload.requireValue(TEST_TABLE.DESCRIPTION)
                                .equals("updated description text")
                )
        );
    }

    @Test
    void patchThrowsInvalidValueWhenPatchContainsUnknownField() {
        assertThrows(InvalidValue.class, () ->
                controller.patch(
                        Map.of(
                                DTOFields.FirstID.toString(), "1",
                                DTOFields.SecondID.toString(), "2"
                        ),
                        Map.of("unknown", "value")
                )
        );

        verify(service, never()).update(any(), any());
    }

    @Test
    void createMergesInitialKeyAndInitialDataAndReturnsCreatedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/entity");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            TestTableRecord created = record(
                    1,
                    2,
                    "created",
                    0,
                    "created description text"
            );

            when(service.createAndGet(any())).thenReturn(created);

            ResponseEntity<EntityController.EntityDTO> response = controller.create(
                    Map.of(
                            DTOFields.FirstID.toString(), "1",
                            DTOFields.SecondID.toString(), "2"
                    ),
                    Map.of(
                            DTOFields.Name.toString(), "created",
                            DTOFields.Description.toString(), "created description text"
                    )
            );

            assertEquals(201, response.getStatusCode().value());
            assertNotNull(response.getBody());

            assertEquals(1, response.getBody().key().get(DTOFields.FirstID.toString()));
            assertEquals(2, response.getBody().key().get(DTOFields.SecondID.toString()));
            assertEquals("created", response.getBody().payload().get(DTOFields.Name.toString()));
            assertEquals("created description text", response.getBody().payload().get(DTOFields.Description.toString()));

            URI location = response.getHeaders().getLocation();
            assertNotNull(location);
            assertTrue(location.toString().contains("first_id=1"));
            assertTrue(location.toString().contains("second_id=2"));

            verify(service).createAndGet(argThat(payload ->
                    payload.requireValue(TEST_TABLE.FIRST_ID).equals(1)
                            && payload.requireValue(TEST_TABLE.SECOND_ID).equals(2)
                            && payload.requireValue(TEST_TABLE.NAME).equals("created")
                            && payload.requireValue(TEST_TABLE.DESCRIPTION).equals("created description text")
            ));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void createWithNullInitialDataUsesOnlyInitialKeyParams() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/entity");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            TestTableRecord created = record(
                    1,
                    2,
                    "created by service",
                    0,
                    "created description text"
            );

            when(service.createAndGet(any())).thenReturn(created);

            ResponseEntity<EntityController.EntityDTO> response = controller.create(
                    Map.of(
                            DTOFields.FirstID.toString(), "1",
                            DTOFields.SecondID.toString(), "2"
                    ),
                    null
            );

            assertEquals(201, response.getStatusCode().value());
            assertNotNull(response.getBody());

            assertEquals(1, response.getBody().key().get(DTOFields.FirstID.toString()));
            assertEquals(2, response.getBody().key().get(DTOFields.SecondID.toString()));

            verify(service).createAndGet(argThat(payload ->
                    payload.requireValue(TEST_TABLE.FIRST_ID).equals(1)
                            && payload.requireValue(TEST_TABLE.SECOND_ID).equals(2)
                            && !payload.assignsField(TEST_TABLE.NAME)
                            && !payload.assignsField(TEST_TABLE.DESCRIPTION)
            ));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void createThrowsInvalidValueWhenInitialDataContainsUnknownField() {
        assertThrows(InvalidValue.class, () ->
                controller.create(
                        Map.of(
                                DTOFields.FirstID.toString(), "1",
                                DTOFields.SecondID.toString(), "2"
                        ),
                        Map.of("unknown", "value")
                )
        );

        verify(service, never()).createAndGet(any());
    }

    @Test
    void deleteDelegatesToServiceDeleteAndReturnsBooleanBody() {
        when(service.delete(any())).thenReturn(true);

        ResponseEntity<Boolean> response = controller.delete(Map.of(
                DTOFields.FirstID.toString(), "1",
                DTOFields.SecondID.toString(), "2"
        ));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody());

        verify(service).delete(argThat(key ->
                key.getValue(TEST_TABLE.FIRST_ID).equals(1)
                        && key.getValue(TEST_TABLE.SECOND_ID).equals(2)
        ));
    }

    @Test
    void deleteThrowsInvalidKeyWhenKeyContainsUnknownField() {
        assertThrows(InvalidKey.class, () ->
                controller.delete(Map.of("unknown", "1"))
        );

        verify(service, never()).delete(any());
    }
}