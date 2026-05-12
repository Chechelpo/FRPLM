package chechelpo.frplm.domain.connection.api_keys;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.UnsupportedAction;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.Map;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.domain.EntityTypes.API_KEYS_URL;

@RestController
@RequestMapping(API_KEYS_URL)
final class SecretController extends ABSEntityController<ApiKeysRecord, SecretService> {
    SecretController(SecretService service) {
        super(EntityTypes.Types.API_KEYS, service);
    }

    @PostMapping(ENTITY_PATH + "/{newKeyName}")
    ResponseEntity<EntityDTO> createNewKey(@PathVariable("newKeyName") String name, @RequestBody(required = true) String key){
        return ResponseEntity.ok(
                wrapEntity(
                        this.service.registerNewKey(name, key)
                )
        );
    }

    @Override
    protected ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) throws URISyntaxException {
        throw new UnsupportedAction("Cannot create secrets via normal entity framework", Severity.USER);
    }
}
