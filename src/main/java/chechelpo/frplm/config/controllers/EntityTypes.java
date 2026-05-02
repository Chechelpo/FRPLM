package chechelpo.frplm.config.controllers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Config for entity controllers, divided by the entity types they provide access for.
 * @apiNote the static final variables need to match the frontend versions.
 */
public final class EntityTypes {
    public static final String BASE = "/api";

    @Contract(pure = true)
    private static @NotNull String concat(String one, String two){
        return one + "/" + two;
    }
    // Chars controllers
    private static final String CHARACTERS_str         = "characters";
    private static final String STARTING_LOCATIONS_str = "startingLocations";
    public static final String CHARACTER_TAGS_str     = "characterTags";

    // Lorebooks
    private static final String LOREBOOKS_str          = "lorebooks";
    private static final String ENTRIES_str            = "entries";
    private static final String ENTRIES_KEYWORDS_str   = "entriesKeywords";

    //Tags
    private static final String TAGS_str              = "tags";
    private static final String KEYWORDS_str          = "keywords";

    // Space controllers
    private static final String WORLDS_str             = "worlds";
    private static final String LOCATIONS_str          = "locations";
    private static final String EDGES_str              = "edges";

    // I know the following variables are stupid, and you'll need to refactor each one if something changes, but bootstrap forces me into it
    // Sorry, future me.
    // Times rewritten : 2

    // Chars controllers
    public static final String CHARACTERS_URL         = BASE + "/" + CHARACTERS_str;
    public static final String STARTING_LOCATIONS_URL = BASE + "/" + STARTING_LOCATIONS_str;
    public static final String CHARACTER_TAGS_URL     = BASE + "/" + CHARACTER_TAGS_str;

    // Lorebooks controllers
    public static final String LOREBOOKS_URL          = BASE + "/" + LOREBOOKS_str;
    public static final String ENTRIES_URL            = BASE + "/" + ENTRIES_str;
    public static final String ENTRIES_KEYWORDS_URL   = BASE + "/" + ENTRIES_KEYWORDS_str;

    //Tags
    public static final String TAGS_URL               = BASE + "/" + TAGS_str;
    public static final String KEYWORDS_URL           = BASE + "/" + KEYWORDS_str;

    // Space controllers
    public static final String WORLDS_URL             = BASE + "/" + WORLDS_str;
    public static final String LOCATIONS_URL          = BASE + "/" + LOCATIONS_str;
    public static final String EDGES_URL              = BASE + "/" + EDGES_str;



    public enum Types {
        TAGS(TAGS_str),
        KEYWORDS(KEYWORDS_str),

        CHARACTER(CHARACTERS_str),
        STARTING_LOCATIONS(STARTING_LOCATIONS_str),
        CHARACTER_TAGS(CHARACTER_TAGS_str),

        LOREBOOKS(LOREBOOKS_str),
        ENTRIES(ENTRIES_str),
        ENTRY_KEYWORDS(ENTRIES_KEYWORDS_str),

        WORLDS(WORLDS_str),
        LOCATIONS(LOCATIONS_str),
        EDGES(EDGES_str)
        ;

        private final String type;

        Types(@NotNull String type) {
            this.type = type;
        }

        public String getEntityType() {
            return type;
        }
    }

}
