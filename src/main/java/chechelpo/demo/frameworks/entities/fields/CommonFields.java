package chechelpo.demo.frameworks.entities.fields;

public enum CommonFields {
    NAME("name")
    ;
    private final String fieldName;

    CommonFields(String fieldName) {
        this.fieldName = fieldName;
    }
    public String getFieldName() {
        return fieldName;
    }
}
