package javax.safetycritical.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface SCJRestricted {
    /** The phase of the mission in which a method may run. */
    public Phase[] value() default { Phase.ALL };
    
    /** Marks whether or not a method may allocate memory. */
    public boolean mayAllocate()    default true;
    
    /** Marks whether or not a method may execute a blocking operation. */
    public boolean maySelfSuspend() default false;
}
