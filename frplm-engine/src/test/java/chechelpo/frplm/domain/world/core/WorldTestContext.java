package chechelpo.frplm.domain.world.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.test_utils.TestText;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static chechelpo.frplm.jooq.generated.Tables.WORLDS;

@TestComponent
@Import(LorebookTestContext.class)
public class WorldTestContext implements DBReload {
    public final WorldService service;
    public final LorebookTestContext context;
    final WorldFieldsHelper fieldsHelper;

    public WorldTestContext(WorldService service, WorldFieldsHelper fieldsHelper, LorebookTestContext context) {
        this.service = service;
        this.fieldsHelper = fieldsHelper;
        this.context = context;
    }

    @Override
    public void reload() {
        context.reload();
    }

    public record Context(List<WorldsRecord> createdRecords) {}

    public Context createWorlds(int amount) {
        List<WorldsRecord> createdRecords = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++)
            createdRecords.add(
                    service.createAndGet(
                            EntityDataPayload.of(WORLDS.NAME, TestText.randomText(ThreadLocalRandom.current().nextLong(), 3 , 200))
                    )
            );

        return new Context(createdRecords);
    }
}
