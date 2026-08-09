package io.github.chechelpo.frplm.core.extras.exporter;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.world.core.WorldMapper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

import static io.github.chechelpo.frplm.jooq.generated.tables.Worlds.WORLDS;

@Component
public class ExporterService {

    private final EntityReaders entityReaders;
    private final ObjectMapper objectMapper;
    private final WorldMapper worldMapper;

    public ExporterService(
            EntityReaders entityReaders,
            ObjectMapper objectMapper,
            WorldMapper worldMapper
    ) {
        this.entityReaders = entityReaders;
        this.objectMapper = objectMapper;
        this.worldMapper = worldMapper;
    }

    public void exportWorld(
            int worldId,
            OutputStream outputStream
    ) throws IOException {
        WorldsRecord world = entityReaders.worlds().require(
                EntityKey.of(WORLDS.ID, worldId)
        );

        try (ZipBuilder zip = new ZipBuilder(outputStream, objectMapper)) {
            JsonNode data = worldMapper.jsonFrom(world, zip);

            zip.addJson("data.json", data);
        }
    }
}