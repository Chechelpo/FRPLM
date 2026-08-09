package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CreationOrder<R extends TableRecord<R>> {
    Optional<String> getZipPath();
    EntityDataPayload<R> payload();

    record Mismatch<R extends TableRecord<R>>(TableField<R, ?> field, Object expected, Object actual) {
        @Override
        public @NonNull String toString() {
            return "Mismatch of created record in field %s. Expected: %s , Got: %s ".formatted(
                    field.getName(),
                    expected,
                    actual
            );
        }
    }

    default List<Mismatch<R>> getMismatches(R createdRecord, Set<TableField<R, ?>> ignoreFields) throws IllegalStateException {
        List<Mismatch<R>> mismatches = new ArrayList<>(payload().assignments().size());
        payload().assignments()
                .forEach(
                        (field, value) -> {
                            if (ignoreFields != null && ignoreFields.contains(field)) return;

                            if (!createdRecord.getValue(field).equals(value))
                                mismatches.add(new Mismatch<>(
                                        field,
                                        createdRecord.getValue(field),
                                        value
                                ));
                        }
                );

        return mismatches;
    }

    default List<Mismatch<R>> getMismatches(R createdRecord) throws IllegalStateException {
        List<Mismatch<R>> mismatches = new ArrayList<>(payload().assignments().size());
        payload().assignments()
                .forEach(
                        (field, value) -> {
                            if (!createdRecord.getValue(field).equals(value))
                                mismatches.add(new Mismatch<>(
                                        field,
                                        createdRecord.getValue(field),
                                        value
                                ));
                        }
                );

        return mismatches;
    }
}
