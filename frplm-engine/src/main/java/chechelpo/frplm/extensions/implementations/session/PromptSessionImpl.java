package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.extensions.api.session.*;
import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.extensions.api.utils.MessagePrompt;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.extensions.implementations.standalone.PromptImpl;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;


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
    public MessagePrompt getNewMessagePrompt() {
        SessionWorld world = session.getWorld();
        SessionLocation currentLocation = world.locationOf(session.getUserCharacter());
        SessionCharacter[] charactersInLocation = currentLocation.getCharactersHere();

        List<LorebookSnapshot> lorebooks = new ArrayList<>(charactersInLocation.length + 2);
        lorebooks.add(world.lorebook());
        lorebooks.add(currentLocation.lorebook());
        Arrays.stream(charactersInLocation).forEach(character -> lorebooks.add(character.lorebook()));

        List<ChatMessage> messages = session.getChatHistory();
        return null;
    }
}
