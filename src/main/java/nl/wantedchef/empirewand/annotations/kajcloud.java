package nl.wantedchef.empirewand.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for KajCloud-style visuals/behavior.
 *
 * This is intended for documentation and code search only; it has no runtime
 * behavior.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface kajcloud {
    // Optional tag value for grouping or notes
    String value() default "style";
}





