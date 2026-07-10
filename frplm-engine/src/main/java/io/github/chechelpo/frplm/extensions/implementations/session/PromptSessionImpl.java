package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.extensions.implementations.standalone.PromptImpl;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptBuilder;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.session.SessionPrompt;

public class PromptSessionImpl extends PromptImpl implements SessionPrompt {
    Session session;
    public PromptSessionImpl(PromptTemplateRecord record, ExtensionContext context, SessionImpl session) {
        super(record, context);
        this.session = session;
    }

    @Override
    public PromptBuilder getNewMessagePrompt() {
        return null;
    }
}
