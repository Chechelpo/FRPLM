package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.jetbrains.annotations.TestOnly;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Component
public final class HostFields extends ABSHelper<ApiHostsRecord, HostService> {
    public HostFields(HostService service) {
        super(service);

        register_field(
                API_HOSTS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                        )
                        .build()
        );
        register_field(
                API_HOSTS.HOST_URL,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .readOnly()
                        )
                        .build()
        );

        Arrays.stream(LLMBackend.values())
                .forEach(backend ->
                            backend.toKey().ifPresent(
                                    (key) -> {
                                if (!service.exists(key)) service.createAndGet(backend.toPayload().orElseThrow());
                            })
                );
    }

    @TestOnly
    void ensureLLMBackendExists() {
        Arrays.stream(LLMBackend.values())
                .forEach(backend ->
                        backend.toKey().ifPresent(
                                (key) -> {
                                    if (!service.exists(key)) service.createAndGet(backend.toPayload().orElseThrow());
                                })
                );
    }
}
