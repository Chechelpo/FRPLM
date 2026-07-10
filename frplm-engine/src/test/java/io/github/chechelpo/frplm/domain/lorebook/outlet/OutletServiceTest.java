package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class OutletServiceTest {
    @Autowired
    OutletService outletService;
    @Autowired
    OutletHelper outletHelper;

    @Test
    void getOrCreateOutlet() {
        int outletAmount = 1_000;
        String[] outletNames = new String[outletAmount];
        //Check creates with the same name
        for (int i = 0; i < outletAmount; i++) {
            outletNames[i] = "outlet" + i;
            int outletId = outletService.getOrCreateOutlet(outletNames[i]);

            Optional<OutletRecord> findResult = outletService.find(EntityKey.of(OUTLET.ID, outletId));
            assertTrue(findResult.isPresent(), "Could not fetch record");
            OutletRecord outletRecord = findResult.get();
            assertEquals(outletRecord.getId(), outletId);
            assertEquals(outletRecord.getOutlet(), outletNames[i]);

            Optional<Integer> id = outletService.getOutletID(outletNames[i]);
            assertTrue(id.isPresent());
            assertEquals(id.get(), outletId);
        }

        List<OutletRecord> outlets = outletService.getAll();
        assertEquals(outletAmount, outlets.size(),
                "Mismatch in number of outlets. Expected " + outletAmount + " but got " + outlets.size()
        );
        for (int i = 0; i < outletAmount; i++) outletService.getOrCreateOutlet(outletNames[i]);

        List<OutletRecord> outlets2 = outletService.getAll();
        assertEquals(outletAmount, outlets2.size(), "Created more outlets on second run");
    }
}