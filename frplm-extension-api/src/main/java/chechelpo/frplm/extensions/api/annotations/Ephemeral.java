package chechelpo.frplm.extensions.api.annotations;

import java.lang.annotation.*;

/**
 * Marks an API value as ephemeral.
 *
 * <p>An ephemeral value is a point-in-time view owned by the engine.
 * Extension code may inspect it during the current callback or method call,
 * but must not retain it beyond that boundary.</p>
 *
 * <p>Invalid retention includes storing it in fields, static fields, caches,
 * collections with longer lifetime, extension configuration, databases,
 * background tasks, futures, or scheduled work.</p>
 *
 * <p>To remember an engine object, store its stable identifier instead.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.TYPE_USE
})
public @interface Ephemeral {
    String value() default "This engine API object is ephemeral. Do not store it beyond the current callback or method call.";
}
