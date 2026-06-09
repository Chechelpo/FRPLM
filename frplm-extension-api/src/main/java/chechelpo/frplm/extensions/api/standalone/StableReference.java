package chechelpo.frplm.extensions.api.standalone;

public interface StableReference {
    String encode();
    /** @return arg1 + arg2 + ... + argn */
    default String concat(String... args) {
        StringBuilder builder = new StringBuilder();

        for (String arg : args) {
            builder.append(arg);
        }

        return builder.toString();
    }
}
