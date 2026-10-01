package dev.fallingcloud.betterinventory.shim;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Local {@code @Nullable}.
 *
 * <p>NeoForge shipped JSR-305's {@code javax.annotation.Nullable}; Fabric does not, and
 * it is not part of the JDK either. This is a drop-in marker with no runtime effect.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface Nullable {
}
