package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.domain.lorebook.outlet.OutletTestContext;
import chechelpo.frplm.interfaces.DBReload;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

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
    @Override
    public void reload(){
        outlets.reload();
    }
}
