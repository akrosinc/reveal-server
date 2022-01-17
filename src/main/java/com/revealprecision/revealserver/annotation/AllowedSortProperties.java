package com.revealprecision.revealserver.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = {AllowedSortPropertiesValidator.class})
@Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedSortProperties {

  String message() default "Request contains unsupported sort element";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String[] value() default "";

  @Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface List {

    AllowedSortProperties[] value();
  }

}
