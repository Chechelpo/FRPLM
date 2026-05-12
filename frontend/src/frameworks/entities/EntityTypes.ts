/* Must reference and be equal to chechelpo.frplm.domain.EntityTypes*/
export enum EntityTypes{
    //Tags
    TAGS = "tags",
    KEYWORD = "keywords",

    // Chars controllers
    CHARACTERS = "characters",
    STARTING_LOCATIONS = `startingLocations`,
    CHARACTER_TAGS = `characterTags`,

    //Lorebooks
    ENTRY = "entries",
    LOREBOOKS = "lorebooks",
    ENTRY_KEYWORD = "entriesKeywords",

    //Space controllers
    WORLDS = "worlds",
    LOCATIONS = "locations",
    EDGES = "edges",

    //Connections
    LLM = "llm",
}