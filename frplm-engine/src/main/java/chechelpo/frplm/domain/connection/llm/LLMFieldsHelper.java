package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.domain.connection.api_hosts.HostService;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.LlmConnection;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@Component
final class LLMFieldsHelper extends ABSControllerAwareHelper<LlmConnectionRecord, LLMService, LLMController> {
    LLMFieldsHelper(LLMService service, LLMController controller) {
        super(service, controller);

        register_field(
                "id",
                LlmConnection.LLM_CONNECTION.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
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
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                                .build()
                        )
                        .build()
        );

        register_field(
                "type",
                LlmConnection.LLM_CONNECTION.HOST_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .setPossibleValues(LLMBackend.getIDs())
                                        .nullable()
                        )
                        .build()
        );

        register_field(
                "modelID",
                LlmConnection.LLM_CONNECTION.MODEL,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                        )
                        .build()
        );
        register_field(
                "api_key",
                LLM_CONNECTION.API_KEY,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER))
                        .build()
        );

    }

}
