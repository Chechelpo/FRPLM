package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.prompts.template.PromptTemplateTestContext;
import chechelpo.frplm.interfaces.DBReload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

@TestComponent
@Import(PromptTemplateTestContext.class)
public class SectionServiceTestContext implements DBReload {
    @Autowired
    public PromptTemplateTestContext prompts;
    @Autowired
    public SectionService sectionService;
    @Autowired
    SectionFieldsHelper fields;

    @Override
    public void reload() {
        prompts.reload();
    }
}
