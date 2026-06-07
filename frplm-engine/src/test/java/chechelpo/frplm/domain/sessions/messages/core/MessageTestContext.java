package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

@TestComponent
@Import(SessionTestContext.class)
public class MessageTestContext implements DBReload {
    @Autowired
    public SessionTestContext sessions;
    @Autowired
    public MessageService service;
    @Autowired
    MessageFieldsHelper fields;

    @Override
    public void reload() {}
}
