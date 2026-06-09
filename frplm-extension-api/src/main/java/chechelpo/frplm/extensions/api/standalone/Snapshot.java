package chechelpo.frplm.extensions.api.standalone;

import chechelpo.frplm.extensions.api.session.ChatMessage;
import chechelpo.frplm.extensions.api.session.Session;
/**
 * A point-in-time immutable view of engine state.
 *
 * <p>Snapshot objects are valid only for the callback invocation, repository
 * query, or session tick that produced them. Extensions must not retain them
 * in fields, caches, static variables, serialized config, databases, or
 * background tasks.</p>
 *
 * <p>Store the corresponding {@code *Ref} instead and resolve it again through
 * the engine API when needed.</p>
 */
public sealed interface Snapshot permits ChatMessage, Session, CharacterSnapshot, ConnectionSnapshot, EntrySnapshot, LocationSnapshot, LorebookSnapshot, PromptSectionSnapshot, PromptSnapshot, WorldSnapshot {}
