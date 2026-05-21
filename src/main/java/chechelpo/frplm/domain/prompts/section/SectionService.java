package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.prompts.template.microservices.TemplateService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.events.crud.CRUDEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.InvalidKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.PromptSection;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.regex.Pattern;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Service
public class SectionService extends EntityService<PromptSectionRecord, SectionStore> {
    private final TemplateService templateService;


    SectionService(SectionStore store, TemplateService templateService, EventBus eventBus) {
        super(store, eventBus);
        this.templateService = templateService;
    }

    public List<PromptSectionRecord> getOrderedSectionsOfTemplate(EntityKey<PromptTemplateRecord> templateKey) {
        return store.getOrderedSections(templateKey);
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<PromptSectionRecord> data, long operationID) {
        EntityKey.Builder<PromptTemplateRecord> builder = EntityKey.builder();
        data.set(
                PromptSection.PROMPT_SECTION.SECTION_ID,
                templateService.getAndIncrement(
                        PROMPT_TEMPLATE.NEXT_SECTION_ID,
                        builder.set(PROMPT_TEMPLATE.ID, data.getValue(PROMPT_SECTION.PROMPT_ID))
                                .build()
                )
        );
        data.set(
                PROMPT_SECTION.POSITION,
                data.getValue(PROMPT_SECTION.SECTION_ID)
        );
        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeDelete(EntityKey<PromptSectionRecord> key, long operationID) {
        if (key.getValue(PROMPT_SECTION.PROMPT_ID) == StandardSections.CHAT_HISTORY.sectionID) {
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
        for (StandardSections section : StandardSections.values()) {
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
