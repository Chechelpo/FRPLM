package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.utils.integrations.T2TClient;
import io.github.chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;

import java.util.Optional;

public class ConnectionImpl extends StandaloneEntity<LlmConnectionRecord> implements ConnectionSnapshot {
    public ConnectionImpl(LlmConnectionRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public ConnectionSnapshot.Reference asReference(){
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
    public Optional<ChatCompletionResponse> generate(String rawRequest) {
        T2TClient client = new T2TClient(context.secrets(), context.hosts());
        return client.generate(
                ChatCompletionRequest.builder()
                        .append(ChatCompletionMessage.user(rawRequest))
                        .build(),
                this.record
        );
    }

    @Override
    public Optional<ChatCompletionResponse> generate(ChatCompletionRequest request) {
        T2TClient client = new T2TClient(context.secrets(), context.hosts());
        return client.generate(
                request,
                this.record
        );
    }


}
