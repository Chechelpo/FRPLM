package io.github.chechelpo.frplm.domain.prompts.section;

import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.NotNull;

public enum DefaultSections {
    SYSTEM_EXPLANATION(
            0,
            ChatCompletionRole.SYSTEM,
            "System explanation",
            null,
            """
You are embedded in a larger world simulation engine. Your job is to play out this world, based on the information you've been given. You must keep narrative constrained exclusively to this knowledge, only inventing things that would be completely inconsequential to the overall world (ex.: villagers small talk).

User is {{user_name}} and you are in charge of the rest of the characters and setting.

Notes:
 - Present characters may remain unmentioned if they don't contribute to this particular scene.
                    """,
            true
    ),
    WORLD_INFO(1,
            ChatCompletionRole.SYSTEM,
            "World information",
            StandardOutlet.WORLD_INFO,
            """
            < {{world_name}} information>
            {{outlet:world_info}}
            </ {{world_name}} information>
            """,
            false
    ),
    LOCATION_INFO(2,
            ChatCompletionRole.SYSTEM,
            "Location information",
            StandardOutlet.LOCATION_INFO,
            """
            < {{location_name}} info>\s
            {{outlet:location_info}}
            </ {{location_name}} info>
            """,
            false
    ),
    CHARACTER_INFO(4,
            ChatCompletionRole.SYSTEM,
            "Character information",
            StandardOutlet.CHARACTER_INFO,
            """
            < Characters information >
            {{outlet:character_info}}
            </ Characters information >
            """,
            false
    ),
    LOREBOOKS(5,
            ChatCompletionRole.SYSTEM,
            "General lorebooks",
            null,
            """
            < Additional information >
            {{outlet:lorebook}}
            </ Additional information >
            """,
            false
    ),
    CHAT_HISTORY(6,
            ChatCompletionRole.USER,
            "Chat history",
            null,
            "unimportant",
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
