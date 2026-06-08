package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.sessions.messages.core.MessageTestContext;
import chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

@TestComponent
@Import({SessionTestContext.class, MessageTestContext.class})
class ResponsesTestContext implements DBReload {
    @Autowired
    public MessageTestContext messages;
    @Autowired
    public SessionTestContext sessions;
    @Autowired
    public ResponseService responses;
    @Autowired
    public GenService genService;

    @Autowired
    GenFieldsHelper genFields;
    @Autowired
    ResponseHelper responseFields;

    @Override
    public void reload() {
        sessions.reload();
    }
}