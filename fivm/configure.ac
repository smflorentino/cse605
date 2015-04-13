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

AC_DEFUN([FP_COMPILE_OPT], [
  AC_MSG_CHECKING(for C compiler option $2)
  echo "int main() { return 0; }" > conftest.c
  AC_TRY_COMMAND(($CC -o conftest conftest.c $CFLAGS $2 && ./conftest) 2>& AC_FD_CC)
  if test $? == 0
  then
    AC_MSG_RESULT(yes)
    $1="$$1 $2"
  else
    AC_MSG_RESULT(no)
  fi
])

AC_DEFUN([FIJI_CHECK_PROG], [
   AC_CHECK_PROG($1, $2, $2)
   AC_SUBST($1)
])

AC_DEFUN([FIJI_PROG_JAVAC], [FIJI_CHECK_PROG([JAVAC], [javac], [javac])])
AC_DEFUN([FIJI_PROG_ECJ], [FIJI_CHECK_PROG([ECJ], [ecj], [ecj])])

AC_INIT(fivm, v0.9.1, elb@fiji-systems.com)

AC_PREREQ(2.52)
AC_CONFIG_SRCDIR([./bin/fivmc])
AC_CONFIG_MACRO_DIR([m4])

VENDOR="Fiji Systems Inc."
COPYRIGHT="(c) 2009, 2010, 2011, 2012, 2013, 2014 Fiji Systems Inc."
VENDOR_WWW="http://www.fiji-systems.com/"
AC_SUBST(VENDOR)
AC_SUBST(COPYRIGHT)
AC_SUBST(VENDOR_WWW)

AC_CANONICAL_TARGET

RTEMS=""

FIJI_JAVA_COMPILER=""
fiji_classpath_fetch=no
FIJI_SCJ_DIR=scj
FIJI_CLASSPATH_DIR=""

AC_ARG_ENABLE([fetch-gnu-classpath], AS_HELP_STRING([--enable-fetch-gnu-classpath@<:@=VERSION@:>@],
                                                    [fetch GNU classpath version VERSION @<:@do not fetch@:>@]),
              [fiji_classpath_fetch=$enableval])

AC_ARG_WITH([java-compiler], AS_HELP_STRING([--with-java-compiler=COMPILER], [java compiler to be used for runtime compilation @<:@guessed@:>@]),
            [FIJI_JAVA_COMPILER=$withval])
AC_ARG_WITH([gnu-classpath], AS_HELP_STRING([--with-gnu-classpath=DIR],
				            [location of GNU classpath install]),
            [FIJI_CLASSPATH_DIR=$withval])
AC_ARG_WITH([scj], AS_HELP_STRING([--with-scj=DIR],
                                  [location of oSCJ source directory]),
            [FIJI_SCJ_DIR=$withval])

AC_PROG_INSTALL

JV_PROG_JAVA
JV_PROG_JAR

FIJI_ECJ_JAR=`pwd`/ecj/ecj-3.5.1.jar
AC_SUBST([FIJI_ECJ_JAR])

if test -z "$RUBY"
then
  AC_MSG_CHECKING(for ruby)
  AC_TRY_COMMAND(ruby --version > /dev/null)
  if test $? == 0
  then
    AC_MSG_RESULT(ruby)
    RUBY=ruby
  else
    AC_TRY_COMMAND(ruby1.8 --version > /dev/null)
    if test $? == 0
    then
      AC_MSG_RESULT(ruby1.8)
      RUBY=ruby1.8
    else
      AC_MSG_ERROR([Could not find ruby; try setting the RUBY environment variable])
    fi
  fi
fi

AC_SUBST(RUBY)

AC_MSG_CHECKING([for usable java compiler])
if test x$FIJI_JAVA_COMPILER != x; then
    AC_MSG_RESULT([using $FIJI_JAVA_COMPILER])
else
    FIJI_JAVA_COMPILER=`pwd`/ecj/ecj
    AC_MSG_RESULT([using bundled ecj])
fi
AC_SUBST([FIJI_JAVA_COMPILER])

CPPFLAGS="$CPPFLAGS -D_REENTRANT -D_GNU_SOURCE"

URL_FETCH=""
FIJI_CLASSPATH_VERSION=""
FIJI_CLASSPATH_FETCH=""
if test x$fiji_classpath_fetch != xno; then
   FIJI_CLASSPATH_FETCH=yes
   FIJI_URL_FETCH_COMMAND([URL_FETCH])
   if test x$fiji_classpath_fetch = xyes; then
       FIJI_CLASSPATH_VERSION=0.97.2
   else
       FIJI_CLASSPATH_VERSION=$fiji_classpath_fetch
   fi
   FIJI_CLASSPATH_DIR=`pwd`/extra/classpath-${FIJI_CLASSPATH_VERSION}-install
fi
AC_SUBST([URL_FETCH])
AC_SUBST([FIJI_CLASSPATH_FETCH])
AC_SUBST([FIJI_CLASSPATH_VERSION])
AC_SUBST([FIJI_CLASSPATH_DIR])

AC_SUBST([FIJI_SCJ_DIR])

# This should probably use libtool or something, but ... for now, this
# hack will do.
FIJI_LIBRARY_SUFFIX=""
FIJI_JNILIB_SUFFIX=""
if test x$FIJI_CLASSPATH_DIR != x; then
    AC_MSG_CHECKING([[platform library suffix]])
    case $host_os in
    darwin*)
        FIJI_LIBRARY_SUFFIX=.dylib
        ;;
    linux*)
        FIJI_LIBRARY_SUFFIX=.so
        ;;
    *)
        AC_MSG_ERROR([[cannot build with classpath on $host_os, contact Fiji Systems Inc.]])
        ;;
    esac
    AC_MSG_RESULT([[$FIJI_LIBRARY_SUFFIX]])

    AC_MSG_CHECKING([[JNI library suffix]])
    case $host_os in
    darwin*)
        FIJI_JNILIB_SUFFIX=.jnilib
        ;;
    *)
        FIJI_JNILIB_SUFFIX=$FIJI_LIBRARY_SUFFIX
        ;;
    esac
    AC_MSG_RESULT([[$FIJI_JNILIB_SUFFIX]])
fi
AC_SUBST([FIJI_LIBRARY_SUFFIX])
AC_SUBST([FIJI_JNILIB_SUFFIX])

AC_CONFIG_FILES([Makefile RealMakefile lib/config.rb lib/host_fivmcrc common/src/com/fiji/fivm/Config.java runtimec/src/fivmr_config_phony.h bin/fivmc_trampoline bin/fijivm_trampoline ecj/ecj Makefile.Test.conf localbin/bottomup])

AC_OUTPUT

chmod 755 ecj/ecj
chmod 755 localbin/bottomup
cp runtimec/src/fivmr_config_phony.h runtimec/src/fivmr_config.h



echo
echo "Fiji VM configuration complete."
echo
echo "Compiler will be installed at:     $prefix/bin/fivmc"
echo "Additional compiler files:         $prefix/share/fivm-$PACKAGE_VERSION"
echo "Java compiler:                     $FIJI_JAVA_COMPILER"
echo
echo "You may wish to add targets by running bin/add-target."
echo
echo "To complete the build and installation for the AOT compiler only, do:"
echo
echo "$ make"
echo "# make install"
echo
echo "To complete the build and installation for both the standalone VM and AOT compiler, do:"
echo
echo "$ make vm"
echo "# make install"
echo
