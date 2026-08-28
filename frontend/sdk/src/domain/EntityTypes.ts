/* Must reference and be equal to chechelpo.frplm.domain.EntityTypes*/
export enum EntityTypes{
    //Tags
    TAGS = "tags",
    KEYWORD = "keywords",

    // Chars controllers
    CHARACTERS = "characters",
    CHARACTER_TAGS = `characterTags`,
    STARTING_LOCATIONS = "startingLocations",

    //Lorebooks
    ENTRY = "entries",
    LOREBOOKS = "lorebooks",
    ENTRY_KEYWORD = "entriesKeywords",
    OUTLETS = "outlets",

    //Space controllers
    WORLDS = "worlds",
    REGIONS = "regions",
    LOCATIONS = "locations",
    EDGES = "edges",

    //PROMPTS
    TEMPLATES = "promptTemplates",
    SECTIONS = "promptSections",

    //Connections
    LLM = "llm",
    API_KEY = "apiKeys",

    //Sessions
    SESSIONS = "sessions",
    MESSAGES = "messages",
    CURRENT_LOCATION = "currentLocations",

    //Session state
    SESSION_CHARACTERS = "sessionCharacters"
}