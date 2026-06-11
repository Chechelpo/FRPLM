package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.domain.prompts.section.DefaultSections;
import chechelpo.frplm.extensions.api.session.*;
import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSectionSnapshot;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.extensions.implementations.standalone.PromptImpl;
import chechelpo.frplm.extensions.implementations.standalone.PromptSectionImpl;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import chechelpo.frplm.utils.prompts.Prompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromptSessionImpl extends PromptImpl implements SessionPrompt {
    Session session;
    public PromptSessionImpl(PromptTemplateRecord record, ExtensionContext context, SessionImpl session) {
        super(record, context);
        this.session = session;
    }

    @Override
    public Prompt.Builder getNewMessagePrompt() {
        SessionWorld world = session.getWorld();
        SessionCharacter userCharacter = session.getUserCharacter();
        SessionLocation currentLocation = world.locationOf(userCharacter);
        SessionCharacter[] present = currentLocation.getCharactersHere();

        Prompt.Builder builder = Prompt.builder();
        for (PromptSectionSnapshot section : this.getSections()){
            if (DefaultSections.CHAT_HISTORY.sectionID == section.reference().sectionId()){
                builder.appendAll(session.getChatHistory());
                continue;
            }
            builder.appendAsSection(section.asCompletionMessage());
        }

        List<LorebookSnapshot> lorebooks = new ArrayList<>(present.length + 2);
        lorebooks.add(world.lorebook());
        lorebooks.add(currentLocation.lorebook());
        lorebooks.addAll(Arrays.stream(present).map(SessionCharacter::lorebook).toList());
        builder.addLorebooks(lorebooks);
        
        return builder;
    }

}
