package chechelpo.frplm.domain.connection.api_keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class SecretServiceTestContext {
    public final SecretService secretService;
    @Autowired SecretFieldsHelper fields;

    public SecretServiceTestContext(SecretService secretService) {
        this.secretService = secretService;
    }
}
