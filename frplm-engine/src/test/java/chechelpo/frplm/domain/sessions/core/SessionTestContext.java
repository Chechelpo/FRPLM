package chechelpo.frplm.domain.sessions.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class SessionTestContext {
    @Autowired
    public SessionService service;
    @Autowired
    SessionFieldsHelper fields;


}
