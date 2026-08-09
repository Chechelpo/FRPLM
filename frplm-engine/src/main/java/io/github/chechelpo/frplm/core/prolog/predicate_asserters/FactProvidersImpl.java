package io.github.chechelpo.frplm.core.prolog.predicate_asserters;

import io.github.chechelpo.frplm.core.prolog.ReservedPredicates;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import it.unibo.tuprolog.core.Fact;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public final class FactProvidersImpl implements FactProviders, SmartInitializingSingleton {
    private final List<FactProvider> providers = List.of(
            new CurrentLocation(),
            new WorldSolver()
    );

    public FactProvidersImpl(){}

    @Override
    public void afterSingletonsInstantiated() {
        chckDuplicates();
    }

    @Override
    public @NonNull @Unmodifiable List<ReservedPredicates> getImplemented(){
        return providers.stream()
                .map(FactProvider::getType)
                .toList();
    }

    @Override
    public @NonNull @Unmodifiable List<Fact> getFacts(SessionImpl session){
        return providers.stream()
                .flatMap(provider -> provider.getFacts(session).stream())
                .toList();
    }

    void chckDuplicates(){
        Set<ReservedPredicates> seen = new HashSet<>();
        for (FactProvider provider : providers){
            if (seen.contains(provider.getType()))
                throw new IllegalStateException("Duplicate provider of " + provider.getType());
            seen.add(provider.getType());
        }
    }


}
