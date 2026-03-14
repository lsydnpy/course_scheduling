package com.xy.course_scheduling.custom.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLogAnn {
    String title() default "";

    BusinessType businessType() default BusinessType.OTHER;

    enum BusinessType {
        INSERT, UPDATE, DELETE, SELECT, OTHER
    }
}
