package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class PromptTemplateTestContext implements DBReload {
    @Autowired
    public TemplateService service;

    @Autowired
    TemplateFieldsHelper fields;

    @Override
    public void reload() {}
}
