package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.standalone.PromptSectionEntitySnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import io.github.chechelpo.frplm.utils.matching.Macro;
import io.github.chechelpo.frplm.utils.matching.ReplacementTarget;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectionManagerTest {
    private static final DSLContext JOOQ =
            DSL.using(SQLDialect.DEFAULT);

    private static final ChatCompletionRole ROLE =
            ChatCompletionRole.SYSTEM;

    private static final PromptSection.InjectAtPosition POSITION =
            new PromptSection.InjectAtPosition.Relative(0);

    @Test
    void explicitConstructorStoresSectionProperties() {
        SectionManager manager = new SectionManager(
                "Unrendered content",
                ROLE,
                POSITION
        );

        assertEquals(
                "Unrendered content",
                manager.getUnrenderedContent()
        );

        assertSame(
                POSITION,
                manager.getInjectionOrder()
        );

        assertFalse(manager.isChatHistorySection());
    }

    @Test
    void sectionManagerRecognizesChatHistory(){
        PromptSectionEntitySnapshot mockSection = mock(PromptSectionEntitySnapshot.class);
        when(mockSection.type()).thenReturn(PromptSectionEntitySnapshot.Type.CHAT_HISTORY);
        when(mockSection.role()).thenReturn(ChatCompletionRole.ASSISTANT);
        when(mockSection.getInjectionOrder()).thenReturn(new PromptSection.InjectAtPosition.Relative(1));

        SectionManager manager = new SectionManager(mockSection);

        assertTrue(manager.isChatHistorySection());
    }

    @Test
    void asCompletionMessageFailsBeforeRendering() {
        SectionManager manager = new SectionManager(
                "Content",
                ROLE,
                POSITION
        );

        assertThrows(
                IllegalStateException.class,
                manager::asCompletionMessage
        );
    }

    @Test
    void renderMacro_RendersSimpleContent(){
        SectionManager manager = new SectionManager(
                "content {{macro}}",
                ROLE,
                POSITION
        );
        MacroManager macroManager = mock(MacroManager.class);

        Macro macro = new Macro("macro");
        Set<ReplacementTarget> macros = Set.of(macro);
        String toInject = "toInject";
        when(macroManager.getTargets()).thenReturn(macros);
        when(macroManager.renderTarget(macro)).thenReturn(Optional.of(toInject));

        manager.injectAtDetectedTargets(macroManager);

        assertEquals("content " + toInject, manager.getRenderedContent());
    }

    @Test
    void renderMacro_rendersNestedContent(){
        SectionManager manager = new SectionManager(
                "content {{macro}}",
                ROLE,
                POSITION
        );
        MacroManager macroManager = mock(MacroManager.class);

        Macro root = new Macro("macro");
        Macro nested = new Macro("nested");
        Set<ReplacementTarget> macros = Set.of(nested, root);
        String rootContent = "toInject {{nested}}";
        String nestedContent = "nested";

        when(macroManager.getTargets()).thenReturn(macros);
        when(macroManager.renderTarget(root)).thenReturn(Optional.of(rootContent));
        when(macroManager.renderTarget(nested)).thenReturn(Optional.of(nestedContent));
        manager.injectAtDetectedTargets(macroManager);

        assertEquals("content " + "toInject " + "nested", manager.getRenderedContent());
    }
}