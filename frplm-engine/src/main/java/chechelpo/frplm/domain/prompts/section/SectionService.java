package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.prompts.template.TemplateService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.InvalidKey;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.PromptSection;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Service
public class SectionService extends EntityService<PromptSectionRecord, SectionStore> {
    private final TemplateService templateService;


    SectionService(SectionStore store, TemplateService templateService, EventBus eventBus) {
        super(store, eventBus);
        this.templateService = templateService;
    }

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
                data.requireValue(PROMPT_SECTION.PROMPT_ID)
        );
        short sectionID = templateService.incrementAndGet(
                    PROMPT_TEMPLATE.NEXT_SECTION_ID,
                    key
        ).orElseThrow(() -> {
            log.error("Prompt template not found while creating section {}", key);
            return new EntityNotFound("Prompt template not found", Severity.USER);
        });

        data.set(PromptSection.PROMPT_SECTION.SECTION_ID, sectionID);
        data.set(PROMPT_SECTION.POSITION, data.requireValue(PROMPT_SECTION.SECTION_ID));

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeDelete(@NotNull EntityKey<PromptSectionRecord> key, long operationID) {
        if (key.getValue(PROMPT_SECTION.PROMPT_ID) == DefaultSections.CHAT_HISTORY.sectionID) {
            log.error("Attempted to delete a chat history off a template");
            throw new InvalidKey("Chat history section can't be deleted", Severity.USER);
        }
        super.beforeDelete(key, operationID);
    }

    /**
     * @implNote skips validation via calling the store directly.
     */
    @TransactionalEventListener
    protected void addStandardSections(CRUDCommittedEvent.@NotNull CreatedEntity<?> createdTemplateEvent) {
        if (createdTemplateEvent.type() != EntityTypes.Types.PROMPT_TEMPLATES) return;

        CRUDCommittedEvent.CreatedEntity<PromptTemplateRecord> createdTemplateEntity =
                (CRUDCommittedEvent.CreatedEntity<PromptTemplateRecord>) createdTemplateEvent;

        short promptID = createdTemplateEntity.key().getValue(PROMPT_TEMPLATE.ID);
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
