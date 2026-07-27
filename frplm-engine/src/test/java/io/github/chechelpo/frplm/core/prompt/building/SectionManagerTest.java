package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.extensions.api.prompts.PromptSection;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionMessage;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectionManagerTest {
/*
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
    void renderingWithNoOutletsPreservesContent() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets());

        SectionManager manager = new SectionManager(
                "Plain section content",
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        ChatCompletionMessage result =
                manager.asCompletionMessage();

        assertEquals(ROLE, result.role());
        assertNull(result.reasoning());
        assertEquals(
                "Plain section content",
                result.content()
        );

        verify(lorebooksManager).getOutlets();
        verifyNoInteractions(promptRenderer);
    }

    @Test
    void injectsAtStandardOutlet() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        StandardOutlet standardOutlet =
                StandardOutlet.LOREBOOK;

        TestOutlet outlet =
                TestOutlet.standard(standardOutlet);

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(outlet));

        when(promptRenderer.renderEligibleAtOutlet(
                standardOutlet.getStableId(),
                lorebooksManager
        )).thenReturn(Optional.of("Injected lore"));

        SectionManager manager = new SectionManager(
                "Before %s after".formatted(
                        standardOutlet.asMacro()
                ),
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(
                "Before Injected lore after",
                manager.asCompletionMessage().content()
        );

        verify(promptRenderer).renderEligibleAtOutlet(
                standardOutlet.getStableId(),
                lorebooksManager
        );
    }

    @Test
    void injectsAtUserDefinedOutlet() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet customOutlet =
                new TestOutlet(91, "user_memories");

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(customOutlet));

        when(promptRenderer.renderEligibleAtOutlet(
                customOutlet.id(),
                lorebooksManager
        )).thenReturn(Optional.of("Custom memories"));

        SectionManager manager = new SectionManager(
                "Before %s after".formatted(
                        StandardOutlet.asMacro(customOutlet.name())
                ),
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(
                "Before Custom memories after",
                manager.asCompletionMessage().content()
        );

        verify(promptRenderer).renderEligibleAtOutlet(
                customOutlet.id(),
                lorebooksManager
        );
    }

    @Test
    void injectsMultipleStandardAndUserDefinedOutlets() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet character =
                TestOutlet.standard(StandardOutlet.CHARACTER_INFO);

        TestOutlet world =
                TestOutlet.standard(StandardOutlet.WORLD_INFO);

        TestOutlet custom =
                new TestOutlet(77, "user_notes");

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(
                        character,
                        world,
                        custom
                ));

        when(promptRenderer.renderEligibleAtOutlet(
                character.id(),
                lorebooksManager
        )).thenReturn(Optional.of("Character information"));

        when(promptRenderer.renderEligibleAtOutlet(
                world.id(),
                lorebooksManager
        )).thenReturn(Optional.of("World information"));

        when(promptRenderer.renderEligibleAtOutlet(
                custom.id(),
                lorebooksManager
        )).thenReturn(Optional.of("User notes"));

        SectionManager manager = new SectionManager(
                """
                %s
                ---
                %s
                ---
                %s
                """.formatted(
                        StandardOutlet.asMacro(character.name()),
                        StandardOutlet.asMacro(world.name()),
                        StandardOutlet.asMacro(custom.name())
                ),
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(
                """
                Character information
                ---
                World information
                ---
                User notes
                """,
                manager.asCompletionMessage().content()
        );

        verify(promptRenderer).renderEligibleAtOutlet(
                character.id(),
                lorebooksManager
        );

        verify(promptRenderer).renderEligibleAtOutlet(
                world.id(),
                lorebooksManager
        );

        verify(promptRenderer).renderEligibleAtOutlet(
                custom.id(),
                lorebooksManager
        );

        verifyNoMoreInteractions(promptRenderer);
    }

    @Test
    void replacesEveryOccurrenceCaseInsensitively() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet outlet =
                TestOutlet.standard(StandardOutlet.LOREBOOK);

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(outlet));

        when(promptRenderer.renderEligibleAtOutlet(
                outlet.id(),
                lorebooksManager
        )).thenReturn(Optional.of("Lore"));

        SectionManager manager = new SectionManager(
                """
                First: {{outlet:lorebook}}
                Second: {{OUTLET:LOREBOOK}}
                Third: {{OuTlEt:LoReBoOk}}
                """,
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(
                """
                First: Lore
                Second: Lore
                Third: Lore
                """,
                manager.asCompletionMessage().content()
        );

        verify(promptRenderer, times(1))
                .renderEligibleAtOutlet(
                        outlet.id(),
                        lorebooksManager
                );
    }

    @Test
    void treatsReplacementCharactersLiterally() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet customOutlet =
                new TestOutlet(101, "special_content");

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(customOutlet));

        String injectedContent =
                "Price: $10, path: C:\\data\\entry";

        when(promptRenderer.renderEligibleAtOutlet(
                customOutlet.id(),
                lorebooksManager
        )).thenReturn(Optional.of(injectedContent));

        SectionManager manager = new SectionManager(
                "Before %s after".formatted(
                        StandardOutlet.asMacro(customOutlet.name())
                ),
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(
                "Before Price: $10, path: C:\\data\\entry after",
                manager.asCompletionMessage().content()
        );
    }

    @Test
    void leavesMacroUnresolvedWhenRendererReturnsEmptyOptional() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet outlet =
                TestOutlet.standard(StandardOutlet.LOREBOOK);

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(outlet));

        when(promptRenderer.renderEligibleAtOutlet(
                outlet.id(),
                lorebooksManager
        )).thenReturn(Optional.empty());

        SectionManager manager = new SectionManager(
                "Before %s after".formatted(
                        StandardOutlet.asMacro(outlet.name())
                ),
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        /*
         * injectEntriesAtDetectedOutlets leaves the macro unresolved.
         * asCompletionMessage removes unresolved macros.
         *
        assertEquals(
                "Before  after",
                manager.asCompletionMessage().content()
        );
    }

    @Test
    void leavesMacroUnresolvedWhenRenderedContentIsBlank() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet outlet =
                new TestOutlet(55, "empty_outlet");

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(outlet));

        when(promptRenderer.renderEligibleAtOutlet(
                outlet.id(),
                lorebooksManager
        )).thenReturn(Optional.of("   "));

        SectionManager manager = new SectionManager(
                "Before %s after".formatted(
                        StandardOutlet.asMacro(outlet.name())
                ),
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(
                "Before  after",
                manager.asCompletionMessage().content()
        );
    }

    @Test
    void renderingDoesNotModifyUnrenderedContent() {
        LorebookManagerImpl lorebooksManager =
                mock(LorebookManagerImpl.class);

        PromptRenderer promptRenderer =
                mock(PromptRenderer.class);

        TestOutlet outlet =
                TestOutlet.standard(StandardOutlet.LOREBOOK);

        String original =
                "Before %s after".formatted(
                        StandardOutlet.LOREBOOK.asMacro()
                );

        when(lorebooksManager.getOutlets())
                .thenReturn(outlets(outlet));

        when(promptRenderer.renderEligibleAtOutlet(
                outlet.id(),
                lorebooksManager
        )).thenReturn(Optional.of("Injected"));

        SectionManager manager = new SectionManager(
                original,
                ROLE,
                POSITION
        );

        manager.injectEntriesAtDetectedOutlets(
                lorebooksManager,
                promptRenderer
        );

        assertEquals(original, manager.getUnrenderedContent());

        assertEquals(
                "Before Injected after",
                manager.asCompletionMessage().content()
        );
    }

    private record TestOutlet(
            int id,
            String name
    ) {
        static TestOutlet standard(StandardOutlet outlet) {
            return new TestOutlet(
                    outlet.getStableId(),
                    outlet.name
            );
        }
    }

    private static Result<Record2<Integer, String>> outlets(
            TestOutlet... outlets
    ) {
        Result<Record2<Integer, String>> result =
                JOOQ.newResult(
                        OUTLET.ID,
                        OUTLET.OUTLET_
                );

        for (TestOutlet outlet : outlets) {
            Record2<Integer, String> record =
                    JOOQ.newRecord(
                            OUTLET.ID,
                            OUTLET.OUTLET_
                    );

            record.values(
                    outlet.id(),
                    outlet.name()
            );

            result.add(record);
        }

        return result;
    }*/
}