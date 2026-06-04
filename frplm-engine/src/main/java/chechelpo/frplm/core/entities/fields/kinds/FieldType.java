package chechelpo.frplm.core.entities.fields.kinds;

public enum FieldType {
        STRING,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN
        ;


        public boolean isValidNumber() {
            return this == BYTE || this == SHORT || this == INTEGER || this == LONG;
        }
        public boolean isValidFloat() {
                return this == FLOAT || this == DOUBLE;
        }
}
