package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.exceptions.runtime.InvalidKey;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import it.unimi.dsi.fastutil.ints.IntComparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(SectionServiceTestContext.class)
class SectionServiceTest {
    @Autowired
    SectionServiceTestContext sections;
    @Autowired
    private SectionService sectionService;

    @BeforeEach
    void setUp() {
        sections.reload();
    }

    @Test
    void testDefaultSectionsCreated() {
        EntityDataPayload<PromptTemplateRecord> createdData = EntityDataPayload.<PromptTemplateRecord>builder()
                .set(PROMPT_TEMPLATE.NAME, "Test")
                .build();

        PromptTemplateRecord prompt = sections.prompts.templates.createAndGet(createdData);
        List<PromptSectionRecord> sectionsOfPrompt = sections.sectionService.getMatching(
                EntityKey.of(PROMPT_SECTION.PROMPT_ID, prompt.getId())
        );
        assertEquals(DefaultSections.values().length, sectionsOfPrompt.size());
        DefaultSections[] defaultSections = DefaultSections.values();

        Arrays.stream(defaultSections).forEach(defaultSection -> {
            Optional<PromptSectionRecord> foundDefault = sectionsOfPrompt.stream()
                    .filter(section -> defaultSection.sectionID == section.getSectionId())
                    .findFirst();
            assertTrue(foundDefault.isPresent(), "Couldn't find default section " + defaultSection.sectionID);
            assertEquals(defaultSection.role.wireValue(), foundDefault.get().getRole());
            assertEquals(defaultSection.content, foundDefault.get().getContent());
        });
    }

    @Test
    void testDeleteDefaultSections() {
        EntityDataPayload<PromptTemplateRecord> createdData = EntityDataPayload.<PromptTemplateRecord>builder()
                .set(PROMPT_TEMPLATE.NAME, "Test")
                .build();

        PromptTemplateRecord prompt = sections.prompts.templates.createAndGet(createdData);
        List<PromptSectionRecord> sectionsOfPrompt = sections.sectionService.getMatching(
                EntityKey.of(PROMPT_SECTION.PROMPT_ID, prompt.getId())
        );
        assertEquals(DefaultSections.values().length, sectionsOfPrompt.size());
        DefaultSections[] defaultSections = DefaultSections.values();

        Arrays.stream(defaultSections).forEach(defaultSection -> {
            EntityKey<PromptSectionRecord> defaultSectionKey = EntityKey.<PromptSectionRecord>builder()
                    .set(PROMPT_SECTION.PROMPT_ID, prompt.getId())
                    .set(PROMPT_SECTION.SECTION_ID, defaultSection.sectionID)
                    .build();
            assertTrue(sections.sectionService.exists(defaultSectionKey), "Couldn't find default section " + defaultSection.sectionID);

            if (!defaultSection.canDelete) {
                assertThrows(
                        InvalidKey.class,
                        () -> sections.sectionService.delete(defaultSectionKey),
                        "Could delete protected section: " + defaultSection.name
                );
            } else assertTrue(sections.sectionService.delete(defaultSectionKey),
                    "Couldn't delete unprotected section: " + defaultSection.name
            );
        });

    }

    @Test
    void getOrderedSectionsOf(){
        EntityDataPayload<PromptTemplateRecord> createdData = EntityDataPayload.<PromptTemplateRecord>builder()
                .set(PROMPT_TEMPLATE.NAME, "Test")
                .build();

        PromptTemplateRecord prompt = sections.prompts.templates.createAndGet(createdData);
        List<PromptSectionRecord> sectionsOfPrompt = sections.sectionService.getMatching(
                EntityKey.of(PROMPT_SECTION.PROMPT_ID, prompt.getId())
        );

        sectionsOfPrompt.sort(Comparator.comparing(PromptSectionRecord::getPosition));
        List<PromptSectionRecord> actualOrderedSections = sections.sectionService.getOrderedSectionsOfTemplate(prompt);

        assertEquals(sectionsOfPrompt, actualOrderedSections);

    }

    @Test
    void testExchangeSections() {
        EntityDataPayload<PromptTemplateRecord> createdData = EntityDataPayload.<PromptTemplateRecord>builder()
                .set(PROMPT_TEMPLATE.NAME, "Test")
                .build();

        PromptTemplateRecord prompt = sections.prompts.templates.createAndGet(createdData);
        List<PromptSectionRecord> sectionsOfPrompt = sections.sectionService.getMatching(
                EntityKey.of(PROMPT_SECTION.PROMPT_ID, prompt.getId())
        );

        short promptID = prompt.getId();
        short falsePromptID = (short) (promptID + 1);
        short falseSectionID = Short.MAX_VALUE;
        for (PromptSectionRecord section : sectionsOfPrompt) {
            for (PromptSectionRecord otherSection : sectionsOfPrompt) {
                int thisPosition = positionOf(section);
                int otherPosition = positionOf(otherSection);
                assertFalse(
                        sectionService.exchange(falsePromptID, section.getSectionId(), otherSection.getSectionId()),
                        "Could exchange sections with false prompt id"
                );
                assertFalse(
                        sectionService.exchange(promptID, section.getSectionId(), falseSectionID),
                        "Could exchange sections with false section id"
                );
                assertTrue(
                        sections.sectionService.exchange(promptID, section.getSectionId(), otherSection.getSectionId()),
                        "Couldn't exchange %s with %s".formatted(section.getName(), otherSection.getName())
                );
                int thisNewPosition = positionOf(section);
                int otherNewPosition = positionOf(otherSection);

                assertEquals(otherPosition, thisNewPosition);
                assertEquals(thisPosition, otherNewPosition);
            }
        }
    }

    int positionOf(PromptSectionRecord section) {
        return sectionService.getValueOf(
                PROMPT_SECTION.POSITION,
                sectionService.keyOf(section)
        ).orElseThrow(() -> new IllegalStateException("Couldn't find position of " + section.getName()));
    }
}