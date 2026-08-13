package io.github.chechelpo.frplm.domain.connection.api_hosts;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Component
public final class HostFields extends EntityFieldsValidator<ApiHostsRecord> {
    public HostFields() {
        super(API_HOSTS);
    }

    @Override
    protected List<FieldInfo<ApiHostsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(API_HOSTS.ID)
                        .build()
        );
    }
}
