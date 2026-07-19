package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import io.github.chechelpo.frplm.domain.prolog.arguments.PrologArgumentType;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.EnumMap;

@Component
public final class EntityTranslator {
    private final EnumMap<PrologArgumentType, PrologEntityTranslator> entityTranslators = new EnumMap<>(PrologArgumentType.class);

    void register(PrologArgumentType type, PrologEntityTranslator translator){
        entityTranslators.putIfAbsent(type, translator);
    }

    public @NonNull String getReferenceFromQualifiedName(PrologArgumentType type, String qualifiedName){
        return entityTranslators.get(type).getIdOfRepresentation(qualifiedName).orElseThrow();
    }

    public @NonNull String getQualifiedNameFromReference(PrologArgumentType type, String reference){
        return entityTranslators.get(type).getQualifiedName(reference).orElseThrow();
    }
}
