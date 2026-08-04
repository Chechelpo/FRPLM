package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_KEYS;

@Component
final class SecretFieldsHelper extends EntityControllerFieldValidator<ApiKeysRecord> {
    SecretFieldsHelper() {
        super(EntityConfigs.Types.API_KEYS);
    }

    @Override
    protected List<FieldInfo<ApiKeysRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(API_KEYS.KEY_ID)
                        .key()
                        .build(),

                FieldInfo.builder(API_KEYS.NAME)
                        .build(),

                FieldInfo.builder(API_KEYS.HOST_ID)
                        .build()
        );
    }

    @Override
    protected List<DTOField<ApiKeysRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(API_KEYS.KEY_ID, "id"),
                DTOField.of(API_KEYS.NAME, "name"),
                DTOField.of(API_KEYS.HOST_ID, "host_id")
        );
    }
}
