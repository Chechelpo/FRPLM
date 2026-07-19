package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import java.util.Optional;

/**
 * Interface in charge of translating qualified names into prolog references, back and forth.
 * <p>
 *     <b>Examples:</b>
 *     <ul>
 *         <li> Character "Alice" has the display name of "Alice" but is represented by prolog fact character(character: 1) being 1 this character's id </li>
 *         <li>
 *             Entry "Alice.Alice's past" has the display name of "Alice's past" but is represented by prolog fact entry(entry: 1,2)
 *             being 1 the lorebook id of the "Alice" lorebook, 2 entry Id
 *         </li>
 *     </ul>
 * </p>
 */
public interface PrologEntityTranslator {
    /**
     * @return this entity prolog representation, empty() if it does not exist.
     */
    Optional<String> getIdOfRepresentation(String argumentName);
    Optional<String> getQualifiedName(String id);
}
