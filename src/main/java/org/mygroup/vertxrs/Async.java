package org.mygroup.vertxrs;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AsyncValidator.class)
@Documented
public @interface Async {
	String message() default "tarace";
	Class<?>[] groups() default { };
	Class<? extends Payload>[] payload() default { };
}
