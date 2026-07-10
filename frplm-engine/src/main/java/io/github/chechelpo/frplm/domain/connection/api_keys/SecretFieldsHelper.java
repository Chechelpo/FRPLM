package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
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
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
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
                        .setConstraints(StringConstraint.builder()
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
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .build()
                        )
                        .build()
        );
    }
}
