package chechelpo.demo.config.controllers;

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

    // Lorebooks
    private static final String LOREBOOKS_str          = "lorebooks";
    private static final String ENTRIES_str            = "entries";


    // Space controllers
    private static final String WORLDS_str             = "worlds";
    private static final String LOCATIONS_str          = "locations";
    private static final String EDGES_str              = "edges";

    //I know the following is stupid, and you'll need to refactor each one if something changes but bootstrap forces me into it

    // Chars controllers
    public static final String CHARACTERS_URL         = BASE + "/" + CHARACTERS_str;
    public static final String STARTING_LOCATIONS_URL = BASE + "/" + STARTING_LOCATIONS_str;

    // Lorebooks controllers
    public static final String LOREBOOKS_URL          = BASE + "/" + LOREBOOKS_str;
    public static final String ENTRIES_URL           = BASE + "/" + ENTRIES_str;

    // Space controllers
    public static final String WORLDS_URL             = BASE + "/" + WORLDS_str;
    public static final String LOCATIONS_URL          = BASE + "/" + LOCATIONS_str;
    public static final String EDGES_URL              = BASE + "/" + EDGES_str;



    public enum Types {
        CHARACTER(CHARACTERS_str),
        STARTING_LOCATIONS(STARTING_LOCATIONS_str),

        LOREBOOKS(LOREBOOKS_str),
        ENTRIES(ENTRIES_str),

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
