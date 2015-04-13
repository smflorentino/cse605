AC_DEFUN([JV_ENSURE_VAR], [
  if test -z "[$]$1"; then
    AC_MSG_ERROR([$2 not found])
  fi
])

AC_DEFUN([JV_REQUIRE_PROG], [
  AC_CHECK_PROG($1, $2, $2)
  JV_ENSURE_VAR($1, $2)
])

AC_DEFUN([JV_REQUIRE_PROGS], [
  AC_CHECK_PROGS($1, $2) 
  if test -z "[$]$1"; then
    AC_MSG_ERROR([$3 not found])
  fi
])

AC_DEFUN([JV_PROG_JAVA], [JV_REQUIRE_PROG(JAVA, java)])

AC_DEFUN([JV_PROG_JAVAC], [
  JV_REQUIRE_PROGS(JAVAC, [javac], [Java compiler])
])

AC_DEFUN([JV_PROG_JAR], [JV_REQUIRE_PROG(JAR, jar)])

AC_DEFUN([JV_TRY_COMPILE], [
  AC_REQUIRE([JV_PROG_JAVAC])
  cat - > $1.java <<EOF
$2
EOF
  AC_TRY_COMMAND($JAVAC $1.java 2>& AC_FD_CC)
  if test $? != 0 || test ! -s $1.class
  then
     AC_MSG_ERROR([Can't compile java programs with $JAVAC])
  fi
])

AC_DEFUN([JV_JAVA_PROP], [
  AC_CACHE_CHECK([Java property $2], jv_cv_$1, [
    AC_REQUIRE([JV_PROG_JAVA])
    JV_TRY_COMPILE(conftestJava, [[
import java.util.StringTokenizer;
public class conftestJava {
  static String metaChars = ";(){}\`'\\"\\\\";
  //" stupid font-lock
  public static void main(String[] args)
  {
    for (int i = 0; i < args.length; i++)
     {
        boolean stopOnPathSep=$3;
        String val = System.getProperty(args[i]);
        if (val == null) {
          System.err.println("could not find" + args[i]);
          System.exit(1);
        }
	if (stopOnPathSep) {
	    StringTokenizer tox=new StringTokenizer(val,":");
	    val=tox.nextToken();
	    if (val.equals(".")) {
	        val=tox.nextToken();
            }
        }
        char[] c = val.toCharArray();
        for (int j = 0; j < c.length; j++)
          {
             if (metaChars.indexOf(c[j]) != -1)
                System.out.print('\\\\');
             System.out.print(c[j]);
          }
        System.out.println("");
      }
  }
}]])
     jv_cv_$1=`AC_TRY_COMMAND(CLASSPATH=. $JAVA conftestJava $2)`
     if test $? != 0 || test -z "$jv_cv_$1" ; then
        AC_MSG_ERROR([Can't execute java programs with $JAVA])
     fi
  ])
  $1="$jv_cv_$1"
  AC_SUBST($1)
])

AC_DEFUN([JV_JAVA_HOME], [JV_JAVA_PROP(JAVA_HOME, java.home, false)])
AC_DEFUN([JV_EXT_DIR], [JV_JAVA_PROP(JAVA_EXT_DIR, java.ext.dirs, true)])
AC_DEFUN([JV_JAVA_LIB_PATH], [JV_JAVA_PROP(JAVA_LIB_PATH, java.library.path, true)])

AC_DEFUN([SCJ_CHECK_PROG], [
   AC_CHECK_PROG($1, $2, $2)
   AC_SUBST($1)
])

AC_DEFUN([SCJ_PROG_JAVAC], [SCJ_CHECK_PROG([JAVAC], [javac], [javac])])
AC_DEFUN([SCJ_PROG_ECJ], [SCJ_CHECK_PROG([ECJ], [ecj], [ecj])])

AC_INIT(scj, 0.7.0, pizlonator@gmail.com)

AC_PREREQ(2.52)
AC_CONFIG_SRCDIR([s3/common/src/javax/realtime/IdentityArraySet.java])

AC_PROG_INSTALL

JV_PROG_JAVA
JV_PROG_JAR
JV_PROG_JAVAC

AC_CONFIG_FILES([Makefile RealMakefile])

AC_OUTPUT

