package io.github.chechelpo.frplm.events.crud;

import io.github.chechelpo.frplm.domain.EntityTypes;
import org.jetbrains.annotations.NotNull;

public sealed interface CRUDEvent permits CRUDCommittedEvent, CRUDDraftEvent {
    long operationID();
    @NotNull EntityTypes.Types type();
}