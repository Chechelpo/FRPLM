package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.UnsupportedAction;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.Map;

import static chechelpo.frplm.domain.EntityTypes.API_KEYS_URL;

@RestController
@RequestMapping(API_KEYS_URL)
final class SecretController extends EntityController<ApiKeysRecord, SecretService> {
    SecretController(SecretService service) {
        super(service);
    }

    private record NewApiKeyRequest(String key) {}
    @PostMapping(  "/new/{host_id}")
    ResponseEntity<EntityDTO> createNewKey(@PathVariable("host_id") Integer host_id, @RequestBody(required = true) NewApiKeyRequest key){
        return ResponseEntity.ok(
                wrapEntity(
                        this.service.registerNewKey(host_id, key.key)
                )
        );
    }

    @Override
    protected ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) throws URISyntaxException {
        throw new UnsupportedAction("Cannot create secrets via normal entity framework", Severity.USER);
    }
}
