AC_DEFUN([FIJI_COMPILE_OPT], [
    AC_MSG_CHECKING([C compiler for option $2])
    fiji_save_CFLAGS=$CFLAGS
    CFLAGS="$CFLAGS $2"
    AC_RUN_IFELSE([AC_LANG_PROGRAM()],
                  [fiji_compile_opt=yes],
                  [fiji_compile_opt=no],
                  [AC_COMPILE_IFELSE([AC_LANG_PROGRAM()],
                                     [fiji_compile_opt=yes],
                                     [fiji_compile_opt=no])])
    CFLAGS=$fiji_save_CFLAGS
    if test x$fiji_compile_opt = xyes; then
        AC_MSG_RESULT([yes])
        $1="$$1 $2"
    else
        AC_MSG_RESULT([no])
    fi
])