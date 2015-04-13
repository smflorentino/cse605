AC_INIT([fivmjnitest], [0.9.0], [fil@fiji-systems.com])

AC_PROG_CC

CPPFLAGS="$CPPFLAGS -D_REENTRANT -D_GNU_SOURCE"
CFLAGS="$CFLAGS -W -Werror -m32 -fno-strict-aliasing"
LDFLAGS="$LDFLAGS -m32"

AC_MSG_CHECKING([for JNI prefix])
JNIPREFIX=`../../../bin/fivmc --jni-prefix`
AC_MSG_RESULT($JNIPREFIX)
AC_SUBST([JNIPREFIX])

AC_MSG_CHECKING([for JNI suffix])
JNISUFFIX=`../../../bin/fivmc --jni-suffix`
AC_MSG_RESULT($JNISUFFIX)
AC_SUBST([JNISUFFIX])

JNILDFLAGS="FAIL"
AC_MSG_CHECKING([how to build jni libs])
my_save_LDFLAGS="$LDFLAGS"
LDFLAGS="-bundle -flat_namespace -undefined suppress $LDFLAGS"
AC_LINK_IFELSE([[
    #include <stdio.h>
    int func() { printf("blah\n"); }
]],[
    LDFLAGS=$my_save_LDFLAGS
    JNILDFLAGS="-bundle -flat_namespace -undefined suppress"
    AC_MSG_RESULT(using $CC options -bundle -flat_namespace -undefined suppress)
],[
    LDFLAGS="-bundle -Wl,-flat_namespace,-undefined,suppress $my_save_LDFLAGS"
    AC_LINK_IFELSE([[
        #include <stdio.h>
        int func() { printf("blah\n"); }
    ]],[
        LDFLAGS=$my_save_LDFLAGS
        JNILDFLAGS="-bundle -Wl,-flat_namespace,-undefined,suppress"
        AC_MSG_RESULT([using $CC options -bundle -Wl,-flat_namespace,-undefined,suppress])
    ],[
        LDFLAGS="-G $my_save_LDFLAGS"
        AC_LINK_IFELSE([[
            #include <stdio.h>
	    int func() { printf("blah\n"); }
        ]],[
            LDFLAGS=$my_save_LDFLAGS
            JNILDFLAGS="-G"
            AC_MSG_RESULT(using $CC option -G)
        ],[
            LDFLAGS="-shared -fPIC $my_save_LDFLAGS"
            AC_LINK_IFELSE([[
	        #include <stdio.h>
                int func() { printf("blah\n"); }
            ]],[
                LDFLAGS=$my_save_LDFLAGS
                JNILDFLAGS="-shared -fPIC"
                AC_MSG_RESULT(using $CC options -shared -fPIC)
            ],[
                LDFLAGS="$my_save_LDFLAGS"
                AC_MSG_ERROR([don't know how to build JNI library])
            ])
        ])
    ])
])

AC_SUBST([JNILDFLAGS])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT

