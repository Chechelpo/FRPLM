package chechelpo.frplm.domain.prompts.template;

import chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class PromptTemplateTestContext implements DBReload {
    @Autowired
    public TemplateService templates;

    @Autowired
    TemplateFieldsHelper fields;

    @Override
    public void reload() {

    }
}
