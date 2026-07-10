package io.github.chechelpo.frplm.domain.connection.api_keys;

import io.github.chechelpo.frplm.domain.connection.api_hosts.HostTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(HostTestContext.class)
public class SecretServiceTestContext implements DBReload {
    public final SecretService secretService;
    @Autowired public HostTestContext hostTestContext;
    @Autowired
    SecretFieldsHelper fields;

    public SecretServiceTestContext(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void reload() {
        hostTestContext.reload();
    }
}
