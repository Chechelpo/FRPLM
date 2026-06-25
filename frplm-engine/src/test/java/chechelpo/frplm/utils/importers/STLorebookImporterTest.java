package chechelpo.frplm.utils.importers;

import chechelpo.frplm.utils.json_mappers.orders.NewEntryOrder;
import chechelpo.frplm.utils.importers.sillytavern.STLorebookImporter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class STLorebookImporterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void getEntries() throws IOException {
        InputStream in = STLorebookImporterTest.class.getResourceAsStream("/imports/eldoria.json");
        assert in != null : "Resource not found: /imports/eldoria.json";
        JsonNode testLorebook = MAPPER.readTree(in);
        System.out.println(testLorebook);

        List<NewEntryOrder> entries = STLorebookImporter.getEntries(testLorebook);
        System.out.println("Results: " + entries);
    }
}