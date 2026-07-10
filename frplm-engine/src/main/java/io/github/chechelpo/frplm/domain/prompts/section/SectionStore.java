package io.github.chechelpo.frplm.domain.prompts.section;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

@Store
final class SectionStore extends EntityStore<PromptSectionRecord> {
    SectionStore(@NotNull DSLContext ctx) {
        super(ctx, PROMPT_SECTION, EntityTypes.Types.SECTIONS);
    }

    @NotNull List<PromptSectionRecord> getOrderedSections(short promptTemplateID) {
        return ctx.selectFrom(PROMPT_SECTION)
                .where(PROMPT_SECTION.PROMPT_ID.eq(promptTemplateID))
                .orderBy(PROMPT_SECTION.POSITION)
                .fetch();
    }


    boolean exchangePositions(short promptID, short sectionID1, short sectionID2) {
        return ctx.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);

            var sections = tx.selectFrom(PROMPT_SECTION)
                    .where(PROMPT_SECTION.PROMPT_ID.eq(promptID))
                    .and(PROMPT_SECTION.SECTION_ID.in(sectionID1, sectionID2))
                    .forUpdate()
                    .fetch();

            PromptSectionRecord section1 = sections.stream()
                    .filter(section -> section.getSectionId().equals(sectionID1))
                    .findFirst()
                    .orElseThrow();

            PromptSectionRecord section2 = sections.stream()
                    .filter(section -> section.getSectionId().equals(sectionID2))
                    .findFirst()
                    .orElseThrow();

            short position1 = section1.getPosition();
            short position2 = section2.getPosition();

            short temporaryPosition;
            try{
                //noinspection DataFlowIssue
                temporaryPosition = (short) (ctx.select(DSL.min(PROMPT_SECTION.POSITION))
                        .from(PROMPT_SECTION)
                        .where(PROMPT_SECTION.PROMPT_ID.eq(promptID))
                        .fetchOne(0, short.class) - 1);
            } catch (NullPointerException e){
                throw new IllegalStateException("Couldn't get a temporary position");
            }

            tx.update(PROMPT_SECTION)
                    .set(PROMPT_SECTION.POSITION, temporaryPosition)
                    .where(PROMPT_SECTION.PROMPT_ID.eq(promptID))
                    .and(PROMPT_SECTION.SECTION_ID.eq(sectionID1))
                    .execute();

            tx.update(PROMPT_SECTION)
                    .set(PROMPT_SECTION.POSITION, position1)
                    .where(PROMPT_SECTION.PROMPT_ID.eq(promptID))
                    .and(PROMPT_SECTION.SECTION_ID.eq(sectionID2))
                    .execute();

            tx.update(PROMPT_SECTION)
                    .set(PROMPT_SECTION.POSITION, position2)
                    .where(PROMPT_SECTION.PROMPT_ID.eq(promptID))
                    .and(PROMPT_SECTION.SECTION_ID.eq(sectionID1))
                    .execute();

            return true;
        });
    }
}
