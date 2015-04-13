AC_INIT(testmake, v0.0.1, fil@fiji-systems.com)

AC_PREREQ(2.52)
AC_CONFIG_SRCDIR([./README])

AC_PROG_INSTALL

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

if test -z "$PERL"
then
  AC_MSG_CHECKING(for perl)
  AC_TRY_COMMAND(perl --version > /dev/null)
  if test $? == 0
  then
    AC_MSG_RESULT(perl)
    PERL=perl
  else
    AC_TRY_COMMAND(perl5 --version > /dev/null)
    if test $? == 0
    then
      AC_MSG_RESULT(perl5)
      PERL=perl5
    else
      AC_MSG_ERROR([Could not find perl; try setting the PERL environment variable])
    fi
  fi
fi

AC_SUBST(PERL)

AC_CONFIG_FILES([Makefile lib/config.rb bin/recall bin/testmake bin/tester bin/summarize bin/inst-recall bin/inst-testmake bin/inst-tester bin/inst-summarize])

AC_OUTPUT

chmod 755 bin/recall bin/testmake bin/tester bin/summarize bin/inst-recall bin/inst-testmake bin/inst-tester bin/inst-summarize
