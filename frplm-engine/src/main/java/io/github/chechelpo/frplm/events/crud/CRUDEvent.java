package io.github.chechelpo.frplm.events.crud;

import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;

public sealed interface CRUDEvent permits CRUDCommittedEvent, CRUDDraftEvent {
    long operationID();
    @NotNull EntityConfigs.Types type();
}