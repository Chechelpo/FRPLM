package chechelpo.frplm.domain;

import ch.qos.logback.classic.Level;
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
    // Chars
    private static final String CHARACTERS_str         = "characters";
    private static final String STARTING_LOCATIONS_str = "startingLocations";
    public static final String CHARACTER_TAGS_str      = "characterTags";

    // Connection
    private static final String API_HOSTS_str          = "apiHosts";
    private static final String API_KEYS_str           = "apiKeys";
    private static final String LLM_CONNECTION_str     = "llm";

    // Lorebooks
    private static final String LOREBOOKS_str          = "lorebooks";
    private static final String ENTRIES_str            = "entries";
    private static final String ENTRIES_KEYWORDS_str   = "entriesKeywords";

    //Prompts
    private static final String TEMPLATES_str          = "promptTemplates";
    private static final String SECTIONS_str           = "promptSections";

    //Tags
    private static final String TAGS_str               = "tags";
    private static final String KEYWORDS_str           = "keywords";

    // Space
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

    // Connection
    public static final String API_KEYS_URL           = BASE + "/" + API_KEYS_str;
    public static final String LLM_CONNECTION_URL     = BASE + "/" + LLM_CONNECTION_str;

    // Lorebooks controllers
    public static final String LOREBOOKS_URL          = BASE + "/" + LOREBOOKS_str;
    public static final String ENTRIES_URL            = BASE + "/" + ENTRIES_str;
    public static final String ENTRIES_KEYWORDS_URL   = BASE + "/" + ENTRIES_KEYWORDS_str;

    // Prompts Controllers
    public static final String PROMPT_TEMPLATES_URL   = BASE + "/" + TEMPLATES_str;
    public static final String SECTIONS_URL           = BASE + "/" + SECTIONS_str;

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

        API_HOSTS(API_HOSTS_str),
        API_KEYS(API_KEYS_str),
        LLM_CONNECTION(LLM_CONNECTION_str),

        PROMPT_TEMPLATES(TEMPLATES_str),
        SECTIONS(SECTIONS_str),

        LOREBOOKS(LOREBOOKS_str),
        ENTRIES(ENTRIES_str),
        ENTRY_KEYWORDS(ENTRIES_KEYWORDS_str),

        WORLDS(WORLDS_str),
        LOCATIONS(LOCATIONS_str),
        EDGES(EDGES_str)
        ;

        private final String type;
        private final Level loggerLevel;

        Types(@NotNull String type) {
            this.type = type;
            this.loggerLevel = Level.INFO;
        }
        Types(@NotNull String type, Level loggerLevel) {
            this.type = type;
            this.loggerLevel = loggerLevel;
        }

        public String getEntityType() {
            return type;
        }
        public Level getLoggerLevel() {
            return loggerLevel;
        }
    }

}
