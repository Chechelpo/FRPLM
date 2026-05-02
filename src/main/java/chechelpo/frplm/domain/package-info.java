/**
 * Contains the application-specific domain layer.
 *
 * <p>The {@code domain} package and its subpackages model the concrete
 * entities of the application: characters, worlds, locations, edges,
 * lorebooks, lorebook entries, tags, character tags, character assets, and
 * starting locations.</p>
 *
 * <h2>Architectural role</h2>
 *
 * <p>This layer binds the generic entity framework to the application's
 * concrete persistence tables, REST endpoints, field metadata, and domain
 * invariants. Most domain modules follow the same structural pattern:</p>
 *
 * <ul>
 *     <li>{@code *Store}: persistence access using jOOQ records and tables;</li>
 *     <li>{@code *Service}: domain operations, creation rules, deletion rules,
 *     and cross-entity coordination;</li>
 *     <li>{@code *Controller}: REST-facing access to the entity service;</li>
 *     <li>{@code *FieldsHelper}: registration of exposed fields, validation
 *     constraints, key fields, read-only fields, and UI-oriented format
 *     metadata.</li>
 * </ul>
 *
 * <h2>Domain modules</h2>
 *
 * <p>The package is organised into several domain areas:</p>
 *
 * <ul>
 *     <li>{@code character}: character records, character tags, character
 *     assets, avatars, and starting locations;</li>
 *     <li>{@code lorebook}: lorebook records and lorebook entries used for
 *     structured world or character knowledge;</li>
 *     <li>{@code space}: worlds, locations, and edges between locations;</li>
 *     <li>{@code tags}: reusable tag definitions and tag lifecycle behaviour.</li>
 * </ul>
 *
 * <h2>Relationship with the framework layer</h2>
 *
 * <p>Domain classes generally extend the generic abstractions from
 * {@code chechelpo.demo.frameworks.entities.microservices}, such as
 * {@code ABSEntityStore}, {@code ABSEntityService},
 * {@code ABSEntityController}, and {@code ABSFieldInstantiationHelper}.
 * The framework layer defines the reusable entity machinery; this domain
 * layer supplies the concrete entity types, jOOQ tables, field definitions,
 * and domain-specific behaviours.</p>
 *
 * <h2>Visibility convention</h2>
 *
 * <p>Most implementation classes are package-private or {@code final} where
 * possible. Public types should normally be exposed only when they must be
 * consumed by other domain modules or by Spring dependency injection across
 * package boundaries.</p>
 */
package chechelpo.frplm.domain;