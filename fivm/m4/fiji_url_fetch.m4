# FIJI_URL_FETCH(URL,
#                [ACTION-IF-FETCHED], [ACTION-IF-NOT-FETCHED],
#                [OUTPUT-FILENAME])
AC_DEFUN([FIJI_URL_FETCH], [
    AC_REQUIRE([FIJI_FIND_URL_FETCHER])
    _fiji_url_fetch_url=AS_ESCAPE($1)
    m4_ifvaln([$4], [_fiji_url_fetch_filename=AS_ESCAPE($4)])dnl
    AC_MSG_NOTICE([fetching URL $1])
    if test x$_fiji_url_fetch_filename != x; then
        : # rm -f $_fiji_url_fetch_filename
    fi
    case $fiji_url_fetcher in
    none)
        AC_MSG_ERROR([[A recognized URL fetching program is required!]])
        ;;
    wget)
        if test x$_fiji_url_fetch_filename != x; then
            _fiji_wget_args="-O $_fiji_url_fetch_filename"
        fi
        wget $_fiji_wget_args "$_fiji_url_fetch_url"
	_fiji_url_fetch_result=$?	
        ;;
    curl)
        if test x$_fiji_url_fetch_filename != x; then
            _fiji_curl_args="-o $_fiji_url_fetch_filename"
        fi
        curl $_fiji_curl_args -f "$_fiji_url_fetch_url"
        _fiji_url_fetch_result=$?
        ;;
    *)
        AC_MSG_ERROR([[An unrecognized URL fetching program was specified!

Please report this to Fiji Systems LLC]])
        ;;
    esac
    AS_IF([test $_fiji_url_fetch_result -eq 0], [$2], [$3])[]dnl
])

# FIJI_URL_FETCH_COMMAND(VARIABLE)
AC_DEFUN([FIJI_URL_FETCH_COMMAND], [
    AC_REQUIRE([FIJI_FIND_URL_FETCHER])
    AC_MSG_CHECKING([[for an appropriate fetch command]])
    case $fiji_url_fetcher in
    none)
        AC_MSG_ERROR([[A recognized URL fetching program is required!]])
        ;;
    wget)
        _fiji_url_fetch_command="wget -O"
        ;;
    curl)
        _fiji_url_fetch_command="curl -f -o"
        ;;
    *)
        AC_MSG_ERROR([[An unrecognized URL fetching program was specified!

Please report this to Fiji Systems LLC]])
        ;;
    esac
    AC_MSG_RESULT([$_fiji_url_fetch_command])
    $1=$_fiji_url_fetch_command
])

AC_DEFUN([FIJI_FIND_URL_FETCHER], [
    AC_CHECK_PROG([_fiji_have_wget], [wget], [yes], [no])
    AC_CHECK_PROG([_fiji_have_curl], [curl], [yes], [no])
    AC_MSG_CHECKING([[for supported URL fetcher]])
    if test x$_fiji_have_wget = xyes; then
        AC_MSG_RESULT([wget])
        fiji_url_fetcher=wget
    elif test x$_fiji_have_curl = xyes; then
        AC_MSG_RESULT([curl])
        fiji_url_fetcher=curl
    else
        AC_MSG_RESULT([not found])
        fiji_url_fetcher=none
    fi
])
