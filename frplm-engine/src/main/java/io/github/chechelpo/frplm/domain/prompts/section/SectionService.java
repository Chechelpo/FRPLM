package io.github.chechelpo.frplm.domain.prompts.section;

import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.domain.prompts.template.TemplateService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidKey;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.jooq.generated.tables.PromptSection;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Service
public class SectionService extends EntityService<PromptSectionRecord, SectionStore> {
    private final TemplateService templateService;

    SectionService(
            SectionStore store,
            FieldValidator<PromptSectionRecord> validator,
            TemplateService templateService,
            EventBus eventBus
    ) {
        super(store, validator, eventBus);
        this.templateService = templateService;
    }

    /**
     * @param template parent
     * @return section records ordered by their position (ascending)
     */
    public List<PromptSectionRecord> getOrderedSectionsOfTemplate(@NotNull PromptTemplateRecord template) {
        return store.getOrderedSections(template.getId());
    }
    @Transactional
    public boolean exchange(short promptID, short sectionID1, short sectionID2)  {
        EntityKey<PromptSectionRecord> key1 = EntityKey.<PromptSectionRecord>builder()
                .set(PROMPT_SECTION.PROMPT_ID, promptID)
                .set(PROMPT_SECTION.SECTION_ID, sectionID1)
                .build();
        EntityKey<PromptSectionRecord> key2 = EntityKey.<PromptSectionRecord>builder()
                .set(PROMPT_SECTION.PROMPT_ID, promptID)
                .set(PROMPT_SECTION.SECTION_ID, sectionID2)
                .build();
        if (!exists(key1)) {
            log.error("Prompt section with key {} not found", key1);
            return false;
        }
        if (!exists(key2)) {
            log.error("Prompt section with key {} not found", key2);
            return false;
        }

        return store.exchangePositions(promptID, sectionID1, sectionID2);
    }
    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<PromptSectionRecord> data, long operationID) {
        EntityKey<PromptTemplateRecord> key = EntityKey.of(PROMPT_TEMPLATE.ID,
                data.require(PROMPT_SECTION.PROMPT_ID)
        );
        short sectionID = templateService.incrementAndGet(
                    PROMPT_TEMPLATE.NEXT_SECTION_ID,
                    key
        ).orElseThrow(() -> {
            log.error("Prompt template not found while creating section {}", key);
            return new EntityNotFound("Prompt template not found", Severity.USER);
        });

        data.set(PromptSection.PROMPT_SECTION.SECTION_ID, sectionID);
        data.set(PROMPT_SECTION.POSITION, data.require(PROMPT_SECTION.SECTION_ID));

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeDelete(@NotNull EntityKey<PromptSectionRecord> key, long operationID) {
        if (!DefaultSections.canDelete(Short.toUnsignedInt(key.require(PROMPT_SECTION.SECTION_ID)))) {
            DefaultSections triedToDeleteSection = DefaultSections.fromSectionID(
                    Short.toUnsignedInt(key.require(PROMPT_SECTION.SECTION_ID)));
            log.error("Attempted to delete a protected default section off a template name {}", triedToDeleteSection);
            throw new InvalidKey("Protected default section can't be deleted", Severity.USER);
        }
        super.beforeDelete(key, operationID);
    }

    /**
     * @implNote skips validation via calling the store directly.
     */
    @TransactionalEventListener
    protected void addStandardSections(CRUDCommittedEvent.@NotNull CreatedEntity<?> createdTemplateEvent) {
        if (createdTemplateEvent.type() != EntityConfigs.Types.PROMPT_TEMPLATES) return;

        CRUDCommittedEvent.CreatedEntity<PromptTemplateRecord> createdTemplateEntity =
                (CRUDCommittedEvent.CreatedEntity<PromptTemplateRecord>) createdTemplateEvent;

        short promptID = createdTemplateEntity.key().getAssignment(PROMPT_TEMPLATE.ID)
                .orElseThrow(ignored ->
                        new UnexpectedException("Expected new record to assign prompt template id ", Severity.SYSTEM)
                );
        log.debug("Inserting standard sections for {}", createdTemplateEntity.record().getValue(PROMPT_TEMPLATE.NAME));
        for (DefaultSections section : DefaultSections.values()) {
            this.store.createAndGet(
                    EntityDataPayload.<PromptSectionRecord>builder()
                            .set(PROMPT_SECTION.PROMPT_ID , promptID)
                            .set(PROMPT_SECTION.SECTION_ID, section.sectionID)
                            .set(PROMPT_SECTION.NAME      , section.name)
                            .set(PROMPT_SECTION.ROLE      , section.role.wireValue())
                            .set(PROMPT_SECTION.POSITION  , section.startingPosition)
                            .set(PROMPT_SECTION.CONTENT   , section.content)
                            .build()
            );
        }
    }
}
