package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.domain.connection.api_hosts.HostService;
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
        );

        register_field(
                "name",
                LlmConnection.LLM_CONNECTION.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(255)
                                .build()
                        )
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
        );

        register_field(
                "model",
                LlmConnection.LLM_CONNECTION.MODEL,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(255)
                        )
        );

        for (LLMBackend backend : LLMBackend.values()){
            EntityKey.Builder<ApiHostsRecord> builder = EntityKey.builder();
            builder.set(API_HOSTS.ID, backend.id);

            if (!hosts.exists(builder.build()))
                hosts.createAndGet(
                        EntityDataPayload.fromValues(Map.of(
                                API_HOSTS.HOST_URL, backend.host,
                                API_HOSTS.ID , backend.id
                        ))
                );
        }
    }

}
