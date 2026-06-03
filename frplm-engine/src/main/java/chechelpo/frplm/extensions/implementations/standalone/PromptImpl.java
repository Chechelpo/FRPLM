package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

public class PromptImpl extends StandaloneEntity<PromptTemplateRecord> implements PromptSnapshot {
    public PromptImpl(PromptTemplateRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Optional<ConnectionSnapshot> getAssignedConnection() {
        if (this.record.getConnectionId() == null) return Optional.empty();

        return context.connections().find(
                EntityKey.of(LLM_CONNECTION.ID, this.record.getConnectionId().intValue())
        ).map(rec -> new ConnectionImpl(rec, this.context));
    }

    @Override
    public Reference reference() {
        return new PromptSnapshot.Reference(this.record.getId());
    }
}
