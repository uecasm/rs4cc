package nz.co.mirality.storage4computercraft.data;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LuaDoc {
    String args() default "";
    String returns() default "";
    int group() default Integer.MAX_VALUE;
    int order() default Integer.MAX_VALUE;
}
