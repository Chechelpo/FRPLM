# Demo document
A no-compromises LLM frontend framework specifically designed for long-term sessions with a focus on a world rather than characters. 

**WARNING** WIP project, only public for recruiters to see.

## About Lorebooks
Lorebooks are logical groupings of entries. They contain only metadata as to how the entries are scanned in generation. Following are the fields we use:
### Lorebook Fields
- **name**: lorebook's name for easier searching. When associated with an entity (Character/lorebook) its name is the entity's name.
- **scan depth** : how far in the prompt will the entries scan for activation. It can be overriden by entries scan depth.
- **Budget**: how many tokens will this lorebook is able to add to the prompt in total. Can be overriden by entries ```ignore budget``` toggle.
### Entries
Entries are the containers of the actual string to inject into a prompt.
#### Entries Fields
- **name**: entry's name for easier search, may be null.
- **content**: text to inject on the condition.
- **Probability**: standard (0 ~ 100%) probability of the entry being activated if its requirements are met.
- **Strategy**: how this entry is scanned, with three possible values.
  - Constant: entry is always going to be active when possible.
  - Normal: entry is activated on keywords match.
  - Embedding: Normal insertion + embedding comparison to previous messages.
    - Embed text: By default the entire entry is embeded for activation, but you can pick a smaller string to embed instead.
- **Conditions** Entries may be from a Character lorebook and as such may be eligible only if the character is present.
  - In location: Entry can only be activated if a character is in a particular location
  - Tag: Entry can only be activated if at least one character/location that subscribes to the tag is present.
  - Entity present: Same as tag but referring to a single character/location.
- **Keywords:** Keywords that activate the entry as per standard lorebook. See Keywords section for clarification on how they work.
- **Outlet:** Up to now, we only discussed when, now we discuss where. By default, entries are injected to one of our standard ```{{outlet::<name>}}``` but its overridable per entry (see outlets section for clarification.)
- **Prevent further recursion:** won't activate other entries
- **Non-recursable:** Entry cannot be activated by other entries.
- **Delay until recursion:** Entry can only be activated by other entries.
- **Delay:** Up to how many turns will this entry be activated.
- **Cooldown:** How much time before the entry is available for activation again.
- **Sticky:** How many messages will the entry persist.
### About keywords
Keywords are not part of the entries, instead relying on an external table entries subscribe to. If they are not detected as existing when an entry requests it, a new one is created. Upon no entry subscribing to a keyword, its automatically deleted.

### On the activation algorithm
Keywords are scanned by their own on previous messages. Tags of characters and characters present are scanned.

We then join on the entries that are eligible and later all entries that subscribe to them are called, and each one evaluates the rest of the conditions on their own, making the overall process a lot easier on resources.

## World modeling
Worlds are not just entries inside a lorebook. Now they are actually modeled via a graph with locations. This allows for
native character presence and space awareness separated from whatever the LLM decides to hallucinate (you'll still have to
tell it that it's hallucinating sometimes tho).

### World fields
- **name**
- **Associated lorebook** Optional lorebook assigned to world.

### Locations
Locations are actual places within a word
#### Location fields
- **name** Unique by location
- **Lorebook** of location.

Now, locations have neighbors. This relationship has the following fields:
- **Travel-time** How much time does the clock advance when moving to the neighbour?
- **description** How the LLM is supposed to introduce the road to the location.

## About Characters
### Character lorebooks
Characters only come with a numeric ID and name as a string value describing them. Everything else is esentially a lorebook entry from their own hidden lorebook.

You want a description? You add an entry to the character lorebook named "description" and insert the value.

Want this description to only be visible when characters that are supposed to know it are on-scene? Add a conditional on the character being present.

Don't want the hassle of allowing each character that knows him? Invert the model, make the entry be available to any character that subscribes to a specific tag.

So, only characters that know him via the tag get that? No, you can make another field called synopsis/rumours/etc. 
Possibilities are endless, see the lorebooks section to make use of it

### Starting locations
To have a character be part of a world, you specify a location. You may also specify:
  - **Reason_why** the entity is there in the first place
  - **TTL** How many messages does this reason why stay injected.

