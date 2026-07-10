package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.domain.connection.api_hosts.HostTestContext;
import io.github.chechelpo.frplm.domain.connection.api_keys.SecretServiceTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

@TestComponent
@Import({HostTestContext.class, SecretServiceTestContext.class})
public class LLMTestContext implements DBReload {
    @Autowired
    public LLMService service;
    @Autowired
    LLMFieldsHelper fields;
    @Autowired
    public HostTestContext hosts;
    @Autowired
    public SecretServiceTestContext secrets;

    @Override
    public void reload() {
        secrets.reload();
        hosts.reload();
    }
}
