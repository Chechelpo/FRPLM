package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.extensions.implementations.standalone.*;
import io.github.chechelpo.frplm.extensions.api.EngineRepository;
import io.github.chechelpo.frplm.extensions.api.standalone.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public final class ExtensionRepository implements EngineRepository, SmartInitializingSingleton {
    private final ExtensionContext context;
    private final Map<Class<?>, ReferenceMapper<?, ?, ?>> mappers;

    public ExtensionRepository(ExtensionContext context) {
        this.context = context;
        this.mappers = Map.ofEntries(
                register(new ConnectionMapper(context.connections())),
                register(new CharacterMapper(context.characters())),
                register(new PromptMapper(context.templates())),
                register(new WorldMapper(context.worlds())),
                register(new LorebookMapper(context.lorebooks())),
                register(new RegionMapper(context.regions())),
                register(new LocationMapper(context.locations())),
                register(new EntryMapper(context.entries())),
                register(new SectionMapper(context.sections()))
        );
    }

    private static Map.Entry<Class<?>, ReferenceMapper<?, ?, ?>> register(ReferenceMapper<?, ?, ?> mapper) {
        return Map.entry(mapper.type(), mapper);
    }

    @Override
    public void afterSingletonsInstantiated() {
        mappers.values().forEach(ReferenceMapper::validate);
    }

    public ExtensionContext getContext() {
        return context;
    }

    @Override
    public <E extends Snapshot<?>> Optional<E> resolve(Class<E> type, String reference) {
        Objects.requireNonNull(type, "Mapper type is null");
        if (reference == null) return Optional.empty();

        return getOrThrowMapperWithoutReference(type).resolve(context, reference);
    }

    @Override
    public <S extends StableReference, E extends Snapshot<S>> Optional<E> get(Class<E> type, S reference) {
        Objects.requireNonNull(type, "Mapper type is null");
        if (reference == null) return Optional.empty();

        return getOrThrowMapperWithReference(type).getWithReference(context, reference);
    }

    @Override
    public @NotNull @Unmodifiable <E extends Snapshot<?>> List<E> getAll(Class<E> ofType) {
        Objects.requireNonNull(ofType, "Type is null");
        return getOrThrowMapperWithoutReference(ofType).getAll(context);
    }

    @SuppressWarnings("unchecked")
    private <E extends Snapshot<?>> ReferenceMapper<?,?,E> getOrThrowMapperWithoutReference(Class<E> type){
        ReferenceMapper<?,?,?> mapper = mappers.get(type);
        if (mapper == null)
            throw new IllegalArgumentException("There's no mapper implemented for snapshots of type: " + type);
        assert mapper.type() == type : "Mapper of type " + type + " has a type mismatch";
        return (ReferenceMapper<?, ?, E>) mapper;
    }

    @SuppressWarnings("unchecked")
    private <S extends StableReference, E extends Snapshot<S>> ReferenceMapper<?,S,E> getOrThrowMapperWithReference(Class<E> type){
        ReferenceMapper<?,?,?> mapper = mappers.get(type);
        if (mapper == null)
            throw new IllegalArgumentException("There's no mapper implemented for snapshots of type: " + type);
        assert mapper.type() == type : "Mapper of type " + type + " has a type mismatch";
        return (ReferenceMapper<?, S, E>) mapper;
    }
}
