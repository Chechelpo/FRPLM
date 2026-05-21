package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.frameworks.entities.microservices.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.springframework.stereotype.Component;

@Component
final class ResponseHelper extends ABSHelper<ResponsesRecord, ResponseService> {
    ResponseHelper(ResponseService service) {
        super(service);
    }
}
