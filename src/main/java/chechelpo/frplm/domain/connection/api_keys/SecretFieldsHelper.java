package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.API_KEYS;

@Component
final class SecretFieldsHelper extends ABSControllerAwareHelper<ApiKeysRecord, SecretService, SecretController> {
    SecretFieldsHelper(SecretService service, SecretController controller) {
        super(service, controller);

        register_field(
                "id",
                API_KEYS.KEY_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                                .build()
                        )
                        .build()
        );

        register_field(
                "name",
                API_KEYS.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(255)
                                .build()
                        )
                        .require()
                        .build()
        );

        register_field(
                "host_id",
                API_KEYS.HOST_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .build()
                        )
                        .build()
        );
    }
}
