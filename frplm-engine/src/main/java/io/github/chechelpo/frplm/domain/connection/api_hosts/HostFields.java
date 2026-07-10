package io.github.chechelpo.frplm.domain.connection.api_hosts;

import io.github.chechelpo.frplm.domain.connection.llm.LLMBackend;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.jetbrains.annotations.TestOnly;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_HOSTS;
import static org.jooq.impl.DSL.max;

@Component
public final class HostFields extends ABSHelper<ApiHostsRecord, HostService> {
    private final DefaultDSLContext dslContext;

    public HostFields(HostService service, DefaultDSLContext dslContext) {
        super(service);

        register_field(
                API_HOSTS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                        )
                        .build(),
                Arrays.stream(LLMBackend.getIDs()).max().orElse(0)
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
        this.dslContext = dslContext;
        restartIdentityAfterCurrentMax();
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

    @TestOnly
    void restartIdentityAfterCurrentMax() {
        Integer nextId = dslContext.select(max(API_HOSTS.ID).plus(1))
                .from(API_HOSTS)
                .fetchOneInto(Integer.class);

        if (nextId == null) {
            nextId = 1;
        }

        dslContext.execute("ALTER TABLE API_HOSTS ALTER COLUMN ID RESTART WITH " + nextId);
    }
}
