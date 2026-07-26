package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({WorldTestContext.class, LorebookTestContext.class})
class WorldServiceTest {
    @Autowired WorldTestContext context;
    @Autowired LorebookTestContext lorebookTestContext;
    @BeforeEach
    void setUp() {
        context.reload();
    }

    @Test
    public void testWorldLorebook(){
        int worldAmount = 100;

        List<EntityDataPayload<WorldsRecord>> worldDatas = new ArrayList<>(worldAmount);
        long seed = 10;

        for (int i = 0; i < worldAmount; i++)
            worldDatas.add(EntityDataPayload.<WorldsRecord>builder()
                    .set(WORLDS.NAME, TestText.randomText(seed + i, 0, 255))
                    .build()
            );

        List<WorldsRecord> records = worldDatas.stream().map(
                data -> assertDoesNotThrow(() -> context.service.createAndGet(data))
        ).toList();

        for (int i = 0; i < worldAmount; i++)
            assertEquals(lorebookTestContext.service.getLorebookOf(records.get(i)).getName(), records.get(i).getName());

        for (int i = 0; i < worldAmount; i++){
            WorldsRecord record = records.get(i);
            LorebooksRecord lorebook = lorebookTestContext.service.getLorebookOf(record);

            assertTrue(this.context.service.delete(context.service.keyOf(record)), "Error deleting character");
            assertFalse(lorebookTestContext.service.find(
                    lorebookTestContext.service.keyOf(lorebook)
            ).isFound(), "Stale lorebook referencing world");
        }
    }
}