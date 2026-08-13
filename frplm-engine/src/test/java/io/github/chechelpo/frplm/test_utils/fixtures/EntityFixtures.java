package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.test_utils.randomMakers.RandomValuesGenerators;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public abstract class EntityFixtures<R extends TableRecord<R>, S extends EntityService<R, ?>> {
    private final S service;
    private final RandomValuesGenerators<R> generator;
    private final Set<TableField<R, ?>> doNotGenerateFields;

    protected EntityFixtures(S service, @NonNull String seed) {
        this.service = service;
        this.generator = new RandomValuesGenerators<>(seed, service.getFieldValidator());
        doNotGenerateFields = doNotGenerateFields();
    }

    protected abstract Set<TableField<R, ?>> doNotGenerateFields();

    public final S service() {
        return service;
    }

    protected final RandomValuesGenerators<R> generator() {
        return generator;
    }

    /**
     * Adds fields to a payload while excluding specified keys before assignment.
     */
    @Contract(mutates = "param1")
    protected void addUntilFullPayloadIgnoringKeys(EntityDataPayload<R> initialData) {
        FieldValidator<R> validator = service.getFieldValidator();
        Arrays.stream(service.getTable().fields())
                .map(field -> (TableField<R, ?>) field)
                .filter(field ->
                        validator.isNotKey(field) &&
                                validator.isNotForeignKey(field) &&
                                !doNotGenerateFields.contains(field)
                )
                .filter(field -> !initialData.assigns(field))
                .forEach(
                        field -> initialData.set(
                                field,
                                generator.getValueFor(field)
                        )
                );
    }

    public <T> R addAndCreateTo(TableField<R,T> field, T value){
        return addAndCreateTo(EntityDataPayload.of(field, value));
    }
    public R addAndCreateTo(EntityDataPayload<R> initialData) {
        addUntilFullPayloadIgnoringKeys(initialData);
        return service.createAndGet(initialData);
    }

    public List<R> addAndCreateList(
            int amount,
            Function<Integer, EntityDataPayload.Builder<R>> builderSupplier
    ) {
        List<R> created = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) {
            R entity = addAndCreateTo(builderSupplier.apply(i).build());
            created.add(
                    entity
            );
        }

        return created;
    }

    public void assertEntityExists(EntityKey<R> key) {
        assertTrue(service.exists(key), "Entity with key " + key + " does not exist");
    }

    public void assertDoesNotExist(EntityKey<R> key) {
        assertFalse(service.exists(key), "Entity with key " + key + " still exists");
    }

    public <T> void assertFieldEquals(T expected, TableField<R, T> field, EntityKey<R> key) {
        assertEntityExists(key);
        assertEquals(
                expected,
                service.require(key).get(field),
                "Mismatch in expected value for field " + field.getName() + " key was: \n" + key
        );
    }

    public <T> void assertFieldEquals(T expected, TableField<R, T> field, R record) {
        assertEquals(
                expected,
                record.get(field),
                "Mismatch in expected value for field " + field.getName() + " record was:\n" + record
        );
    }
}
