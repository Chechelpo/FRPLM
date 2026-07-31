package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.extensions.api.standalone.TagSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;

public class TagImpl extends StandaloneEntity<TagsRecord> implements TagSnapshot {
    public TagImpl(TagsRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public Reference asReference() {
        return new Reference(record.getId());
    }
}
