package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;

public enum DefaultSections {
    WORLD_INFO(1,
            ChatCompletionRole.SYSTEM,
            "World information",
            StandardOutlet.WORLD_INFO,
            " <START> World Information \n"
                    + StandardOutlet.WORLD_INFO.asMacro()
    ),
    LOCATION_INFO(2,
            ChatCompletionRole.SYSTEM,
            "Location information",
            StandardOutlet.LOCATION_INFO,
            "<START> location info \n"
                    + StandardOutlet.LOCATION_INFO.asMacro()
    ),
    CHARACTER_INFO(4,
            ChatCompletionRole.SYSTEM,
            "Character information",
            StandardOutlet.CHARACTER_INFO,
            "<START> Character information \n "
                    + StandardOutlet.CHARACTER_INFO.asMacro()
    ),
    CHAT_HISTORY(5,
            ChatCompletionRole.USER,
            "Chat history",
            StandardOutlet.CHAT_HISTORY,
            StandardOutlet.CHAT_HISTORY.asMacro()
    ),
    ;
    public final short sectionID;
    public final short startingPosition;
    public final ChatCompletionRole role;
    public final StandardOutlet outlet;
    public final String name;
    public final String content;

    DefaultSections(int sectionID, ChatCompletionRole role, String name, StandardOutlet standardOutlet, String content) {
        this.sectionID = (short) sectionID;
        this.role = role;
        this.name = name;
        this.startingPosition = (short) sectionID;
        this.outlet = standardOutlet;
        this.content = content;
    }

    public static short maxReservedSectionID(){
        int max = 0;
        for (DefaultSections section : DefaultSections.values()) {
            max = Math.max(max, section.sectionID);
        }
        return (short) max;
    }
}
