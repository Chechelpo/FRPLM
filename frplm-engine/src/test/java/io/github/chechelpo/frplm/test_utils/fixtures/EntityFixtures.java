package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.test_utils.randomMakers.RandomValuesGenerators;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public abstract class EntityFixtures<R extends TableRecord<R>, S extends EntityService<R, ?>> {
    private final S service;
    private final RandomValuesGenerators<R> generator;
    private final Set<TableField<R, ?>> doNotGenerateFields;

    EntityFixtures(S service, EntityFixtureFactory fixtures, @NonNull String seed) {
        this.service = service;
        this.generator = new RandomValuesGenerators<>(seed, service.getFieldValidator());
        this.doNotGenerateFields = doNotGenerateFields();
    }

    protected abstract Set<TableField<R, ?>> doNotGenerateFields();

    protected abstract List<Consumer<EntityDataPayload<R>>>  getFunctionsToAssignForeignFields(
            EntityDataPayload<R> sample
    );

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
    private void addUntilFullPayloadIgnoringKeys(EntityDataPayload<R> initialData) {
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

    public final <T> R addAndCreateTo(TableField<R,T> field, T value){
        return addAndCreateTo(EntityDataPayload.of(field, value));
    }

    public final R addAndCreateTo(EntityDataPayload<R> initialData) {
        addUntilFullPayloadIgnoringKeys(initialData);
        getFunctionsToAssignForeignFields(initialData)
                .forEach(consumer -> consumer.accept(initialData));

        return service.createAndGet(initialData);
    }

    public final List<R> addAndCreateList(
            int amount,
            Function<Integer, EntityDataPayload.Builder<R>> builderSupplier
    ) {
        if (amount <= 0) throw new IllegalArgumentException("Invalid amount: " + amount);
        List<EntityDataPayload<R>> toCreate = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) toCreate.add(builderSupplier.apply(i).build());

        if (toCreate.isEmpty()) return List.of();

        EntityDataPayload<R> sample = toCreate.getFirst();
        getFunctionsToAssignForeignFields(sample)
                .forEach(toCreate::forEach);

        return toCreate.stream()
                .map(this::addAndCreateTo)
                .toList();
    }

    public final void assertEntityExists(EntityKey<R> key) {
        assertTrue(service.exists(key), "Entity with key " + key + " does not exist");
    }

    public final void assertDoesNotExist(EntityKey<R> key) {
        assertFalse(service.exists(key), "Entity with key " + key + " still exists");
    }

    public final <T> void assertFieldEquals(T expected, TableField<R, T> field, EntityKey<R> key) {
        assertEntityExists(key);
        assertEquals(
                expected,
                service.require(key).get(field),
                "Mismatch in expected value for field " + field.getName() + " key was: \n" + key
        );
    }

    public final <T> void assertFieldEquals(T expected, TableField<R, T> field, R record) {
        assertEquals(
                expected,
                record.get(field),
                "Mismatch in expected value for field " + field.getName() + " record was:\n" + record
        );
    }
}
