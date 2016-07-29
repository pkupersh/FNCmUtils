package ru.blogic.fn.utils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by sam158 on 29.06.2016.
 * mark class as executed utility. Marked class must extend FnExecutor's class
 *
 */
@Target(value= ElementType.TYPE)
@Retention(value= RetentionPolicy.RUNTIME)
public @interface Utility {
    /**
     * what method name must be in -ut parameter for execute this utility. Will be used only this value if defined
     * @return String method name. By default return "" value  - use class name for method name.
     */
    String value() default "";
}
