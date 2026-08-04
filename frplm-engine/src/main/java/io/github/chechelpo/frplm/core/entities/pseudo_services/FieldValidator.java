package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Validates field assignments contained in entity payloads and entity keys.
 *
 * <p>A {@code FieldValidator} is responsible for verifying that:</p>
 *
 * <ul>
 *     <li>All assigned fields are registered and permitted for the operation.</li>
 *     <li>Assigned values satisfy the corresponding field constraints.</li>
 *     <li>Entity keys contain only registered key fields.</li>
 *     <li>Complete keys contain every required key field.</li>
 *     <li>Update payloads do not assign key or otherwise non-writable fields.</li>
 *     <li>Creation payloads contain all fields required for entity creation.</li>
 * </ul>
 *
 * <p>Validation returns a {@link FieldActionResult} rather than throwing for
 * ordinary validation failures. The result contains either the validated
 * payload or a structured failure describing the offending field or value.</p>
 *
 * @param <R> the jOOQ record type whose fields are validated
 */
public interface FieldValidator<R extends TableRecord<R>> {

    /**
     * Validates the assignments contained in an entity key.
     *
     * <p>This method verifies that every assigned field is a registered key
     * field and that every assigned value satisfies the corresponding field
     * configuration and constraints.</p>
     *
     * <p>The key is not required to contain every registered key field.
     * Consequently, this method may be used to validate partial keys. Use
     * {@link #validateFullKey(EntityKey)} when all key fields must be present.</p>
     *
     * @param key the entity key to validate
     * @return a successful result containing {@code key}, or a failure
     *         describing the first invalid field or value
     */
    FieldActionResult<R, EntityKey<R>> validateKey(EntityKey<R> key);

    /**
     * Validates a complete entity key.
     *
     * <p>This method performs all checks from {@link #validateKey(EntityKey)}
     * and additionally verifies that every registered key field is assigned.</p>
     *
     * @param key the entity key to validate
     * @return a successful result containing {@code key}, or a failure if the
     *         key contains an invalid assignment or omits a required key field
     */
    FieldActionResult<R, EntityKey<R>> validateFullKey(EntityKey<R> key);

    /**
     * Validates a payload used to update an existing entity.
     *
     * <p>This method verifies that every assigned field is registered, writable
     * during update operations, and assigned a value satisfying its configured
     * type and constraints.</p>
     *
     * <p>Key fields are not permitted in an update payload.</p>
     *
     * <p>This method does not require every registered field to be present,
     * because update payloads may contain only the fields being modified.</p>
     *
     * @param payload the update payload to validate
     * @return a successful result containing {@code payload}, or a failure
     *         describing the first invalid, unknown, read-only, or key field
     */
    FieldActionResult<R, EntityDataPayload<R>> validateDataPayload(
            EntityDataPayload<R> payload
    );

    /**
     * Validates a payload used to create a new entity.
     *
     * <p>This method verifies that every assigned field is registered and that
     * every assigned value satisfies the corresponding field configuration and
     * constraints.</p>
     *
     * <p>After validating explicitly assigned values, implementations may apply
     * configured default values to fields that are absent from the payload.
     * Once defaults have been applied, every field marked as required for
     * creation must be present.</p>
     *
     * @param payload the creation payload to validate and potentially augment
     *                with configured default values
     * @return a successful result containing {@code payload}, or a failure
     *         describing the first invalid assignment or missing required field
     */
    FieldActionResult<R, EntityDataPayload<R>> validateDataCreationPayload(
            EntityDataPayload<R> payload
    );

    default List<EntityKey<R>> keysOf(@NonNull List<R> records){
        return records.stream()
                .map(this::keyOf)
                .toList();
    }

    Table<R> getTable();
    boolean isKey(TableField<R, ?> field);
    EntityKey<R> keyOf(R record);

    EntityKey<R> extractKeysFrom(EntityDataPayload<R> payload);
}
