package io.featurehub.mr.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this is tagged on an operation then it will allow a  user who needs to reset their password through,
 * otherwise if a user needs to reset their password they are treated as unauthorized
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllowedDuringPasswordReset {
}
