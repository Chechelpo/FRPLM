package chechelpo.demo.llm.legacy;

import chechelpo.demo.asset.AppDirectory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConnectionPersister {
    private ConnectionPersister() {}
    private static final Path SUB_DIRECTORY = AppDirectory.APP_DIRECTORY.resolve("LLM_Connection");
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void saveAll() throws IOException {
        ConnectionProfile[] profiles = ConnectionRegistry.getConnectionProfiles();
        Files.createDirectories(SUB_DIRECTORY);

        for (ConnectionProfile profile : profiles) {
            if (profile == null) continue;

            Path file = SUB_DIRECTORY.resolve(profile.getID() + ".json");
            mapper.writeValue(file.toFile(), profile);
        }
    }

    public static void loadAll() throws IOException {
        if (!Files.exists(SUB_DIRECTORY)) return;

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(SUB_DIRECTORY, "*.json")) {

            for (Path file : stream) {
                ConnectionProfile profile =
                        mapper.readValue(file.toFile(), ConnectionProfile.class);
                ConnectionRegistry.loadProfile(profile);
            }
        }
    }
}
