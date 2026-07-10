package io.github.chechelpo.frplm.domain.connection.api_hosts;

import io.github.chechelpo.frplm.interfaces.DBReload;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class HostTestContext implements DBReload{
    public final HostService service;
    final HostFields fields;

    public HostTestContext(HostService service, HostFields fields) {
        this.service = service;
        this.fields = fields;
    }

    @Override
    public void reload() {
        fields.ensureLLMBackendExists();
        fields.restartIdentityAfterCurrentMax();
    }
}
