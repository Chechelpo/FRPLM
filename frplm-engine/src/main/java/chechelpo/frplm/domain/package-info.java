/**
 * Contains the application-specific domain layer.
 *
 * <p>The {@code domain} package and its subpackages modelID the concrete
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
 *     <li>{@code *Service}: domain operations, data validation, creation rules, deletion rules,
 *     and cross-entity coordination via events or calling other services;</li>
 *     <li>{@code *Controller}: REST-facing access to the entity service, defines DTOs, coerces incoming REST calls;</li>
 *     <li>{@code *FieldsHelper}: helpers for initiating fields of services and controllers.</li>
 * </ul>
 *
 * <h2>Relationship with the framework layer</h2>
 *
 * <p>Domain classes generally extend the generic abstractions, such as
 * {@code EntityStore}, {@code ABSEntityService},
 * {@code EntityController}, and {@code ABSFieldInstantiationHelper}.
 * The framework layer defines the reusable entity machinery; this domain
 * layer supplies the concrete entity types, jOOQ tables, field definitions,
 * and domain-specific behaviours.</p>
 *
 * <h2>Visibility convention</h2>
 * Domain services/ controllers must be kept unaware of whatever is outside the domain folder. There are obvious exceptions
 * such as some utils and the base classes.
 */
package chechelpo.frplm.domain;