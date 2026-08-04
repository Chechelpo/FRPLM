package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import io.github.chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class OutletTestContext implements DBReload {
    @Autowired
    StableRecordCreator creator;
    public final OutletService outletService;
    final OutletHelper outletHelper;
    final OutletStore outletStore;

    OutletTestContext(
            OutletService outletService,
            OutletHelper outletHelper,
            OutletStore outletStore
    ) {
        this.outletService = outletService;
        this.outletHelper = outletHelper;
        this.outletStore = outletStore;
    }

    @Override
    public void reload() {
        creator.run();
    }
}