package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.domain.prompts.template.utils.Prompt;

public enum StandardSections {
    WORLD_INFO(1,
            Prompt.Role.SYSTEM,
            "World information",
            StandardOutlet.WORLD_INFO,
            " <START> World Information \n"
                    + StandardOutlet.WORLD_INFO.name
    ),
    LOCATION_INFO(2,
            Prompt.Role.SYSTEM,
            "Location information",
            StandardOutlet.LOCATION_INFO,
            "<START> location info \n"
                    + StandardOutlet.LOCATION_INFO.name
    ),
    CHARACTER_INFO(4,
            Prompt.Role.SYSTEM,
            "Character information",
            StandardOutlet.CHARACTER_INFO,
            "<START> Character information \n "
                    + StandardOutlet.CHARACTER_INFO.name
    ),
    CHAT_HISTORY(5,
            Prompt.Role.USER,
            "Chat history",
            StandardOutlet.CHAT_HISTORY,
            StandardOutlet.CHAT_HISTORY.name
    ),
    ;
    public final short sectionID;
    public final short startingPosition;
    public final Prompt.Role role;
    public final StandardOutlet outlet;
    public final String name;
    public final String content;

    StandardSections(int sectionID, Prompt.Role role, String name, StandardOutlet standardOutlet, String content) {
        this.sectionID = (short) sectionID;
        this.role = role;
        this.name = name;
        this.startingPosition = (short) sectionID;
        this.outlet = standardOutlet;
        this.content = content;
    }

    public static short maxReservedSectionID(){
        int max = 0;
        for (StandardSections section : StandardSections.values()) {
            max = Math.max(max, section.sectionID);
        }
        return (short) max;
    }
}
