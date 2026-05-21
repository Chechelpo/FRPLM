package chechelpo.frplm.domain.connection.api_hosts;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.API_HOSTS;

@Component
public final class HostFields extends ABSHelper<ApiHostsRecord, HostService> {
    public HostFields(HostService service) {
        super(service);

        register_field(
                API_HOSTS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                        )
                        .build()
        );
        register_field(
                API_HOSTS.HOST_URL,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .readOnly()
                        )
                        .build()
        );
    }
}
