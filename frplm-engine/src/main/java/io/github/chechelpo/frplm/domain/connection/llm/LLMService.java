package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.core.entities.pseudo_services.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.connection.api_hosts.HostService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@Service
public class LLMService extends EntityService<LlmConnectionRecord, LLMStore> {
    private final HostService hostService;

    LLMService(LLMStore store, DTOMapper<LlmConnectionRecord> mapper, EventBus eventBus, HostService hostService) {
        super(store, mapper, eventBus);
        this.hostService = hostService;
    }

    public EntityKey<LlmConnectionRecord> keyOf(Integer connectionId) {
        return EntityKey.of(LLM_CONNECTION.ID, connectionId);
    }

    @Transactional(readOnly = true)
    public RecordFindResult<LlmConnectionRecord> fromTemplate(@NotNull PromptTemplateRecord template) {
        return this.find(EntityKey.of(LLM_CONNECTION.ID, template.getConnectionId()));
    }

    @Transactional
    public ApiHostsRecord assignHost(int conId, String url) {
        ApiHostsRecord hostsRecord = hostService.createOrGetWithHost(url);
        this.update(
                        keyOf(conId),
                        EntityDataPayload.of(LLM_CONNECTION.HOST_ID, hostsRecord.getId().shortValue())
                )
                .ifEntityNotFoundThrow(Severity.USER)
                .ifFailureThrow("Couldn't assign LLM connection host " + url, Severity.SYSTEM);

        return hostsRecord;
    }
}
