/*---------------------------------------------------------------------*\
 *
2 * aicas GmbH, Karlsruhe, Germany 2007
 *
 * This code is provided to the JSR 302 group for evaluation purpose
 * under the LGPL 2 license from GNU.  This notice must appear in all
 * derived versions of the code and the source must be made available
 * with any binary version.  Viewing this code does not prejudice one
 * from writing an independent version of the classes within.
 *
 * $Source: /home/cvs/jsr302/RI/src/java/javax/safetycritical/annotate/SCJAllowed.java,v $
 * $Revision: 1.5 $
 * $Author: jjh $
 * Contents: Java source of HIJA Safety Critical Java annotation SCJAllowed
 *
\*---------------------------------------------------------------------*/

package javax.safetycritical.annotate;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static javax.safetycritical.annotate.Level.LEVEL_0;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation distinguishes methods, classes, and fields that may
 * be accessed from within safety-critical Java programs.  In some
 * implementations of the safety-critical Java specification,
 * elements which are not declared with the @SCJAllowed annotation
 * (and are therefore not allowed in safety-critical application
 * software) are present within the declared class hierarchy.  These
 * are necessary for full compatibility with standard edition Java, the
 * Real-Time Specification for Java, and/or for use by the
 * implementation of infrastructure software.
 * <p>
 * The value field equals LEVEL_0 for elements that may be used within
 * safety-critical Java applications targeting levels 0, 1, or 2.
 * <p>
 * The value field equals LEVEL_1 for elements that may be used within
 * safety-critical Java applications targeting levels 1 or 2.
 * <p>
 * The value field equals LEVEL_2 for elements that may be used within
 * safety-critical Java applications targeting level 2.
 * <p>
 * Absence of this annotation on a given Class, Field, Method, or
 * Constructor declaration indicates that the corresponding element
 * may not be accessed from within a compliant safety-critical Java
 * application.
 */
@SCJAllowed
@Retention(CLASS)
@Target({TYPE, FIELD, METHOD, CONSTRUCTOR})
public @interface SCJAllowed
{ 
    public Level value() default LEVEL_0;
    public boolean members() default false;
}
