package chechelpo.frplm.core.entities.pseudo_services;

import static org.junit.jupiter.api.Assertions.*;

class EntityControllerTest {
    private TestService service;
    private TestController controller;
    enum DTOFields{
        FirstID,
        SecondID,
        Name,
        Description,
        Counter
    }
    @BeforeEach
    void setUp() {
        service = mock(TestService.class);

        when(service.getType()).thenReturn(EntityTypes.Types.TEST);
        when(service.isKey(TEST_TABLE.FIRST_ID)).thenReturn(true);
        when(service.isKey(TEST_TABLE.SECOND_ID)).thenReturn(true);
        when(service.isKey(TEST_TABLE.NAME)).thenReturn(false);
        when(service.isKey(TEST_TABLE.DESCRIPTION)).thenReturn(false);
        when(service.isKey(TEST_TABLE.COUNTER)).thenReturn(false);

        controller = new TestController(service);

        controller.registerPublicField(TEST_TABLE.FIRST_ID, "firstId", FieldInfo.numberField(FieldType.INTEGER).build().format);
        controller.registerPublicField(TEST_TABLE.SECOND_ID, "secondId", FieldInfo.numberField(FieldType.INTEGER).build().format);
        controller.registerPublicField(TEST_TABLE.NAME, "name", FieldInfo.stringField().build().format);
        controller.registerPublicField(TEST_TABLE.DESCRIPTION, "description", FieldInfo.stringField().build().format);
        controller.registerPublicField(TEST_TABLE.COUNTER, "counter", FieldInfo.numberField(FieldType.INTEGER).build().format);
    }
}