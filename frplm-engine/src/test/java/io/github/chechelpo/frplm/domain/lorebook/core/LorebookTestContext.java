package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@TestComponent
@Import(OutletTestContext.class)
public class LorebookTestContext implements DBReload {
    public final LorebookService service;
    public final OutletTestContext outlets;

    final LorebookStore store;
    final LorebookFieldsHelper fields;
    LorebookTestContext(
            LorebookService service,
            LorebookStore store,
            LorebookFieldsHelper fields,
            OutletTestContext outlet
    ) {
        this.service = service;
        this.store = store;
        this.fields = fields;
        this.outlets = outlet;
    }

    public List<LorebooksRecord> createLorebooks(Long seed, int number){
        if (seed == null) seed = ThreadLocalRandom.current().nextLong();
        List<LorebooksRecord> lorebooks = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            lorebooks.add(
                    service.createAndGet(
                            EntityDataPayload.of(Lorebooks.LOREBOOKS.NAME, "Lorebook"+i)
                    )
            );
        }
        return lorebooks;
    }

    @Override
    public void reload(){
        outlets.reload();
    }
}
