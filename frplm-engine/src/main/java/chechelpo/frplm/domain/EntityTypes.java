package chechelpo.frplm.domain;

import ch.qos.logback.classic.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Config for entity controllers, divided by the entity types they provide access for.
 * @apiNote the static final variables need to match the frontend versions.
 */
public final class EntityTypes {
    public static final String API_BASE = "/api";

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
    private static final String OUTLET_str             = "outlets";

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

    // Sessions
    private static final String SESSIONS_str           = "sessions";
    private static final String MESSAGES_str           = "messages";
    private static final String LLM_GEN_str            = "llm";
    private static final String RESPONSES_str          = "responses";

    //State tracker
    private static final String MOVEMENTS_str          = "movements";
    private static final String CURRENT_LOCATIONS_str  = "currentLocations";

    // I know the following variables are stupid, and you'll need to refactor each one if something changes, but bootstrap forces me into it
    // Sorry, future me.
    // Times rewritten : 2

    // Chars controllers
    public static final String CHARACTERS_URL         = API_BASE + "/" + CHARACTERS_str;
    public static final String STARTING_LOCATIONS_URL = API_BASE + "/" + STARTING_LOCATIONS_str;
    public static final String CHARACTER_TAGS_URL     = API_BASE + "/" + CHARACTER_TAGS_str;

    // Connection
    public static final String API_KEYS_URL           = API_BASE + "/" + API_KEYS_str;
    public static final String LLM_CONNECTION_URL     = API_BASE + "/" + LLM_CONNECTION_str;

    // Lorebooks controllers
    public static final String LOREBOOKS_URL          = API_BASE + "/" + LOREBOOKS_str;
    public static final String ENTRIES_URL            = API_BASE + "/" + ENTRIES_str;
    public static final String ENTRIES_KEYWORDS_URL   = API_BASE + "/" + ENTRIES_KEYWORDS_str;
    public static final String OUTLET_URL             = API_BASE + "/" + OUTLET_str;

    // Prompts Controllers
    public static final String PROMPT_TEMPLATES_URL   = API_BASE + "/" + TEMPLATES_str;
    public static final String SECTIONS_URL           = API_BASE + "/" + SECTIONS_str;

    //Tags
    public static final String TAGS_URL               = API_BASE + "/" + TAGS_str;
    public static final String KEYWORDS_URL           = API_BASE + "/" + KEYWORDS_str;

    // Space controllers
    public static final String WORLDS_URL             = API_BASE + "/" + WORLDS_str;
    public static final String LOCATIONS_URL          = API_BASE + "/" + LOCATIONS_str;
    public static final String EDGES_URL              = API_BASE + "/" + EDGES_str;

    // Sessions
    public static final String SESSIONS_URL           = API_BASE + "/" + SESSIONS_str;
    public static final String MESSAGES_URL           = API_BASE + "/" + MESSAGES_str;
    public static final String LLM_GEN_URL            = API_BASE + "/" + LLM_GEN_str;
    public static final String CURRENT_LOCATIONS_URL  = API_BASE + "/" + CURRENT_LOCATIONS_str;

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
        OUTLET(OUTLET_str),

        WORLDS(WORLDS_str),
        LOCATIONS(LOCATIONS_str),
        EDGES(EDGES_str),

        SESSIONS(SESSIONS_str, Level.DEBUG),
        MESSAGES(MESSAGES_str, Level.DEBUG),
        LLM_GEN(LLM_GEN_str, Level.DEBUG),
        RESPONSES(RESPONSES_str, Level.DEBUG),

        MOVEMENTS(MOVEMENTS_str, Level.DEBUG),
        CURRENT_LOCATIONS(CURRENT_LOCATIONS_str, Level.DEBUG),
        TEST_ENTITY("Test_Entity", Level.TRACE);
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
