package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import chechelpo.frplm.utils.generation.GenerationEntryPoint;

public class ConnectionImpl extends StandaloneEntity<LlmConnectionRecord> implements ConnectionSnapshot {
    public ConnectionImpl(LlmConnectionRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public ConnectionSnapshot.Reference reference(){
        return new ConnectionSnapshot.Reference(record.getId());
    }
    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public String getModelID() {
        return record.getModel();
    }

    @Override
    public boolean hasApiKey() {
        return context.secrets().hasApiKey(this.record);
    }

    @Override
    public ChatCompletionResponse generate(String rawRequest) {
        return GenerationEntryPoint.generateNonStreamingResponse(rawRequest, this.record, context);
    }

    @Override
    public ChatCompletionResponse generate(ChatCompletionRequest request) {
        return GenerationEntryPoint.generateNonStreamingResponse(
                request,
                this.record,
                context
        );
    }


}
