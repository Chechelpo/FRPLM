package chechelpo.frplm.domain.connection.llm.microservices;

import chechelpo.frplm.domain.connection.api_hosts.HostService;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.LlmConnection;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.stereotype.Component;

import java.util.Map;

import static chechelpo.frplm.jooq.generated.Tables.API_HOSTS;
import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@Component
final class LLMFieldsHelper extends ABSControllerAwareHelper<LlmConnectionRecord, LLMService, LLMController> {
    LLMFieldsHelper(LLMService service, LLMController controller, HostService hosts) {
        super(service, controller);

        register_field(
                "id",
                LlmConnection.LLM_CONNECTION.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .build()
        );

        register_field(
                "name",
                LlmConnection.LLM_CONNECTION.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(255)
                                .build()
                        )
                        .build()
        );

        register_field(
                "type",
                LlmConnection.LLM_CONNECTION.TYPE,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .setPossibleValues(LLMBackend.getIDs())
                                        .nullable()
                        )
                        .build()
        );

        register_field(
                "modelID",
                LlmConnection.LLM_CONNECTION.MODEL,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(255)
                        )
                        .build()
        );
        register_field(
                "api_key",
                LLM_CONNECTION.API_KEY,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER))
                        .build()
        );

        for (LLMBackend backend : LLMBackend.values()){
            if (backend.stable_id == null || backend.host == null) continue;
            EntityKey.Builder<ApiHostsRecord> builder = EntityKey.builder();
            builder.set(API_HOSTS.ID, backend.stable_id);

            if (!hosts.exists(builder.build()))
                hosts.createAndGet(
                        EntityDataPayload.fromValues(Map.of(
                                API_HOSTS.HOST_URL, backend.host.toString(),
                                API_HOSTS.ID , backend.stable_id
                        ))
                );
        }
    }

}
