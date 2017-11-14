package net.redpipe.example.wiki.shiroJdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ApiUpdateValidator.class)
@Documented
public @interface ApiUpdateValid {
    String message() default "Bad request payload";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
    
    /** The keys to require */
	String[] value();
}
