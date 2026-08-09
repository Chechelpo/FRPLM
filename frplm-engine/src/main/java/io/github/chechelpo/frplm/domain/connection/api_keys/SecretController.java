package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.Map;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.API_KEYS_URL;

@RestController
@RequestMapping(API_KEYS_URL)
final class SecretController extends EntityController<ApiKeysRecord, SecretServiceImpl> {
    SecretController(SecretServiceImpl service, DTOMapper<ApiKeysRecord> record) {
        super(service, record);
    }

    private record NewApiKeyRequest(String key) {}
    @PostMapping(  "/new/{host_id}")
    ResponseEntity<EntityDTO> createNewKey(@PathVariable int host_id, @RequestBody NewApiKeyRequest key){
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
