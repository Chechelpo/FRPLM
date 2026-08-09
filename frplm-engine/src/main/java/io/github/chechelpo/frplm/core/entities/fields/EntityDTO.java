package io.github.chechelpo.frplm.core.entities.fields;

import java.util.Map;

public interface EntityDTO {
    String type();
    Map<String, Object> key();
    Map<String, Object> payload();
}
