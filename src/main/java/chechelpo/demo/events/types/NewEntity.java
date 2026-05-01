package chechelpo.demo.events.types;

import chechelpo.demo.config.controllers.EntityTypes;
import org.jooq.TableRecord;

public record NewEntity(EntityTypes.Types type, TableRecord<?> record) implements Event {}
