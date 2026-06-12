package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.NotNull;

public enum DefaultSections {
    SYSTEM_EXPLANATION(
            0,
            ChatCompletionRole.SYSTEM,
            "System explanation",
            null,
            """
                    You are embedded in a larger world simulation engine. Your job is to play out this world, based on the
                    information you've been given. You must keep narrative constrained exclusively to this knowledge, only
                    inventing things that would be completely inconsequential to the overall world (ex.: villagers small talk).
                    """,
            true
    ),
    WORLD_INFO(1,
            ChatCompletionRole.SYSTEM,
            "World information",
            StandardOutlet.WORLD_INFO,
            " <START> World Information \n"
                    + StandardOutlet.WORLD_INFO.asMacro(),
            false
    ),
    LOCATION_INFO(2,
            ChatCompletionRole.SYSTEM,
            "Location information",
            StandardOutlet.LOCATION_INFO,
            "<START> location info \n"
                    + StandardOutlet.LOCATION_INFO.asMacro(),
            false
    ),
    CHARACTER_INFO(4,
            ChatCompletionRole.SYSTEM,
            "Character information",
            StandardOutlet.CHARACTER_INFO,
            "<START> Character information \n "
                    + StandardOutlet.CHARACTER_INFO.asMacro(),
            false
    ),
    CHAT_HISTORY(5,
            ChatCompletionRole.USER,
            "Chat history",
            StandardOutlet.CHAT_HISTORY,
            StandardOutlet.CHAT_HISTORY.asMacro(),
            false
    ),
    ;
    public final short sectionID;
    public final short startingPosition;
    public final boolean canDelete;
    public final ChatCompletionRole role;
    public final StandardOutlet outlet;
    public final String name;
    public final String content;

    DefaultSections(int sectionID, ChatCompletionRole role, String name, StandardOutlet standardOutlet, String content, boolean canDelete) {
        this.sectionID = (short) sectionID;
        this.role = role;
        this.name = name;
        this.startingPosition = (short) sectionID;
        this.outlet = standardOutlet;
        this.content = content;
        this.canDelete = canDelete;
    }

    public static short maxReservedSectionID() {
        int max = 0;
        for (DefaultSections section : DefaultSections.values()) {
            max = Math.max(max, section.sectionID);
        }
        return (short) max;
    }

    public static @NotNull DefaultSections fromSectionID(int sectionID) {
        for (DefaultSections section : DefaultSections.values()) {
            if (section.sectionID == sectionID) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown section ID: " + sectionID);
    }

    public static boolean canDelete(int sectionID) {
        for (DefaultSections section : DefaultSections.values())
            if (section.sectionID == sectionID)
                return section.canDelete;

        return true;
    }
}
