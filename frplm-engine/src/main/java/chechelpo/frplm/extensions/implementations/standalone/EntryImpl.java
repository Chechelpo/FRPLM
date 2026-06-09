package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;

public class EntryImpl extends StandaloneEntity<EntryRecord> implements EntrySnapshot  {
    protected EntryImpl(EntryRecord record, ExtensionContext context) {
        super(record, context);
    }

}
