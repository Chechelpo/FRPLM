package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import chechelpo.frplm.jooq.generated.tables.PromptSection;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;

import java.util.List;
import java.util.Optional;

public class PromptSectionImpl extends StandaloneEntity<PromptSectionRecord> implements PromptSectionSnapshot {
    protected PromptSectionImpl(PromptSectionRecord record, ExtensionContext context) {
        super(record, context);
    }
}
