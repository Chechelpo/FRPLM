package io.github.chechelpo.frplm.extensions.implementations.standalone;

import org.jooq.TableRecord;

public abstract class StandaloneEntity<R extends TableRecord<R>> {
    protected final R record;
    protected final ExtensionContext context;

    protected StandaloneEntity(R record, ExtensionContext context) {
        this.record = record;
        this.context = context;
    }

    public R getRecord() {
        return record;
    }
}
