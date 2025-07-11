package org.apache.seata.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Description of the tool parameters
 * @author xb2555
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * Parameter description
     */
    String description() default "";

    /**
     * Required
     */
    boolean required() default false;

    /**
     * Example values for parameters
     */
    String example() default "";

    /**
     * Example value type
     */
    Class<?>[] exampleValueClassName() default {};
}
