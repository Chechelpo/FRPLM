package chechelpo.demo.frameworks.entities.fields.constraints;

import chechelpo.demo.frameworks.entities.fields.kinds.FieldKind;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;

public final class BoolConstraints extends Constraints<FieldKind.BooleanKind> {
    public BoolConstraints(boolean read_only) {
        super(read_only, FieldType.BOOLEAN);
    }
}
