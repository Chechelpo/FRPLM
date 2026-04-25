package chechelpo.demo.frameworks.entities.fields.kinds;
/** Field type. Can either be a primitive or array of values ({@link #ENUM})*/
public enum FieldType {
        STRING,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN,
        /** Meant to represent an array of options */
        ENUM
        ;


        public boolean isValidNumber() {
            return this == BYTE || this == SHORT || this == INTEGER || this == LONG;
        }
        public boolean isValidFloat() {
                return this == FLOAT || this == DOUBLE;
        }
}
