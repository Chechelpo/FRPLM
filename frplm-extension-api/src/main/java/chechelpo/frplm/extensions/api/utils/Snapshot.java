package chechelpo.frplm.extensions.api.utils;

import java.time.Instant;

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
public interface Snapshot {}