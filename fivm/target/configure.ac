# foo

AC_INIT([fivmcrc], [0.9.0], [elb@fiji-systems.com])

TARGET=$host
AC_CANONICAL_HOST
GNUHOST=$host
if test x$TARGET = x; then
  TARGET=$host
fi
AC_SUBST(TARGET)
AC_SUBST(GNUHOST)

# FIXME: ditch this garbage.  detect the CPU according to what assembly
# code it can execute.
ARCH=$host_cpu
OS=$host_os

AC_SUBST([ARCH])
AC_SUBST([OS])

AC_PROG_CC
AC_PROG_RANLIB

CPPFLAGS="$CPPFLAGS $XTRA_CPPFLAGS -D_REENTRANT -D_GNU_SOURCE"
CFLAGS="$CFLAGS"
LDFLAGS="$LDFLAGS $XTRA_LDFLAGS"

AC_CHECK_PROGS([JAVA], [java], [])
if test x$JAVA = x; then
    AC_MSG_ERROR([a java runtime environment is required])
fi

SYNCTYPE=""
SYNCTYPEMAC=""
OSFLAVOR=""
PTRSIZE=""

fiji_dyn_loading=yes
fiji_use_sse=yes
fiji_timestamp=""
fiji_pause=""
fiji_self_man_mem=no
fiji_finalization=yes
fiji_pthread_pip=""
fiji_pthread_pcep=""
fiji_ipv6=""
fiji_force_sem_init=no
fiji_rtems_grub=""
fiji_platform_sync=""
fiji_backend_arch="yes"
fiji_arm_hardfloat="yes"

fiji_warn_cross=no

AC_ARG_WITH([rtems], AS_HELP_STRING([--with-rtems=DIR], [location of the RTEMS build to be used]),
            [RTEMS=$withval])
AC_ARG_WITH([rtems-bsp], AS_HELP_STRING([--with-rtems-bsp], [RTEMS BSP to build against]),
            [RTEMSBSP=$withval])
AC_ARG_WITH([android], AS_HELP_STRING([--with-android=DIR], [location of the Android build to be used]),
             [ANDROID=$withval])
AC_ARG_WITH([android-product], AS_HELP_STRING([--with-android-product=PRODUCT],
                                              [name of the Android product to build against]),
            [ANDROIDPRODUCT=$withval])

AC_ARG_ENABLE([android], AS_HELP_STRING([--enable-android], [enable Android build]),
              [fiji_android=$enableval], [fiji_android=no])
AC_ARG_ENABLE([dyn-loading], AS_HELP_STRING([--disable-dyn-loading], [disable dynamic loading]),
              [fiji_dyn_loading=$enableval], [fiji_dyn_loading=yes])
AC_ARG_ENABLE([sse], AS_HELP_STRING([--disable-sse], [disable SSE instruction generation; use --enable-sse=force to force SSE support]),
              [fiji_use_sse=$enableval], [fiji_use_sse=yes])
AC_ARG_ENABLE([pthread-pip], AS_HELP_STRING([--enable-pthread-pip],
                                            [force-enable (disable) priority inheritance mutexes @<:@guessed@:>@]),
              [fiji_pthread_pip=$enableval])
AC_ARG_ENABLE([pthread-pcep], AC_HELP_STRING([--enable-pthread-pcep],
                                             [force-enable (disable) priority ceiling mutexes @<:@guessed@:>@]),
              [fiji_pthread_pcep=$enableval])
AC_ARG_ENABLE([pointer-size], AS_HELP_STRING([--enable-pointer-size=BYTES], [target system pointer size in bytes]),
              [PTRSIZE=$enableval])
AC_ARG_ENABLE([ipv6], AC_HELP_STRING([--enable-ipv6], [force-enable (disable) IPv6 support @<:@guessed@:>@]),
              [fiji_ipv6=$enableval])
AC_ARG_ENABLE([osflavor], AS_HELP_STRING([--enable-osflavor=FLAVOR], [OS flavor (POSIX, RTEMS, etc.) @<:@guessed@:>@]),
              [OSFLAVOR=$enableval])
AC_ARG_ENABLE([synctype], AS_HELP_STRING([--enable-synctype=METHOD], [synchronization method for this target @<:@guessed@:>@]),
              [SYNCTYPE=$enableval])
AC_ARG_ENABLE([timestamp], AS_HELP_STRING([--enable-timestamp=METHOD], [timestamp method for this target @<:@guessed@:>@]),
              [fiji_timestamp=$enableval])
AC_ARG_ENABLE([log-pagesize], AS_HELP_STRING([--enable-log-pagesize=SIZE], [the base-2 logarithm of the system page size in bytes @<:@guessed@:>@]),
              [LOGPAGESIZE=$enableval])
AC_ARG_ENABLE([pause], AS_HELP_STRING([--enable-pause=METHOD], [pause method for this target @<:@guessed@:>@]),
              [fiji_pause=$enableval])
AC_ARG_ENABLE([backend-arch], AS_HELP_STRING([--enable-backend-arch=ARCH], [enable (or disable) the native backend, and specify which to use (x86 is currently the only option)]),
              [fiji_backend_arch=$enableval])
AC_ARG_ENABLE([sem-init], AS_HELP_STRING([--enable-sem-init], [target platform has working sem_init()]),
             [fiji_force_sem_init=$enableval])
AC_ARG_ENABLE([rtems-grub], AS_HELP_STRING([--enable-rtems-grub], [build RTEMS with linker options appropriate for booting with GRUB on x86]),
              [fiji_rtems_grub="-Wl,-Ttext,0x00100000"])
AC_ARG_ENABLE([arm-hardfloat], AS_HELP_STRING([--disable-arm-hardfloat], [Disable hardware floating point on certain ARM platforms]),
              [fiji_arm_hardfloat=$enableval])

EXTRA_CPPFLAGS=""

# Look for the host OS

case $host_os in
rtems*)
    host_os=rtems
    if test x$RTEMS = x; then
        AC_MSG_WARN([[

We appear to be building an RTEMS runtime, but no RTEMS build directory
was provided.  You may wish to use --with-rtems=DIR.
]])
    else
        fiji_rtems_found=no
        AC_MSG_CHECKING([for an RTEMS build])
        for fiji_rtems_dir in $RTEMS $RTEMS/$TARGET/$RTEMSBSP; do
            if test -d $fiji_rtems_dir/lib -a -d $fiji_rtems_dir/lib/include; then
                fiji_rtems_found=yes
                break
            fi
        done
        if test $fiji_rtems_found = no; then
            AC_MSG_RESULT([not found])
            if test -d $RTEMS -a x$RTEMSBSP != x; then
                AC_MSG_ERROR([[$RTEMSBSP does not seem to be a valid BSP for $TARGET]])
            else
                AC_MSG_ERROR([[$RTEMS does not seem to be an RTEMS build]])
            fi
        fi
        RTEMS=$fiji_rtems_dir
        AC_MSG_RESULT([$RTEMS])
        CPPFLAGS="-B$RTEMS/lib -qrtems $CPPFLAGS -I$RTEMS/lib/include"
        LDFLAGS="-B$RTEMS/lib -specs bsp_specs -qrtems $fiji_rtems_grub $LDFLAGS"
    fi
    if test x$OSFLAVOR = x; then
        OSFLAVOR=RTEMS
    fi
    fiji_dyn_loading=no
    if test x$fiji_use_sse = xyes; then
        fiji_use_sse=no
    fi
    fiji_self_man_mem=yes
    fiji_finalization=no
    JNIPREFIX=
    JNISUFFIX=
    ;;
darwin*)
    host_os=darwin
    if test x$OSFLAVOR = x; then
        OSFLAVOR=POSIX
    fi
    AC_DEFINE(FIVMSYS_JNIPRE,["lib"],[prefix for JNI])
    AC_DEFINE(FIVMSYS_JNISUF,[".jnilib"],[suffix for JNI])
    JNIPREFIX=lib
    JNISUFFIX=.jnilib
    ;;
linux*)
    host_os=linux
    if test x$OSFLAVOR = x; then
        OSFLAVOR=POSIX
    fi
    AC_DEFINE(FIVMSYS_JNIPRE,["lib"],[prefix for JNI])
    AC_DEFINE(FIVMSYS_JNISUF,[".so"],[suffix for JNI])
    JNIPREFIX=lib
    JNISUFFIX=.so
    ;;
*)
    if test x$OSFLAVOR = x; then
        OSFLAVOR=POSIX
    fi
    AC_DEFINE(FIVMSYS_JNIPRE,["lib"],[prefix for JNI])
    AC_DEFINE(FIVMSYS_JNISUF,[".so"],[suffix for JNI])
    JNIPREFIX=lib
    JNISUFFIX=.so
    ;;
esac

fiji_osflavor=`echo $OSFLAVOR | tr a-z A-Z`

AC_SUBST([OSFLAVOR])
AC_SUBST([JNIPREFIX])
AC_SUBST([JNISUFFIX])

AC_MSG_CHECKING([whether to self-manage heap memory])
if test x$fiji_self_man_mem = xyes; then
    AC_MSG_RESULT([yes])
    GSELFMANMEM="--g-self-man-mem"
else
    AC_MSG_RESULT([no])
fi
AC_SUBST(GSELFMANMEM)

AC_MSG_CHECKING([whether to enable finalization])
if test x$fiji_finalization = xyes; then
    AC_MSG_RESULT([yes])
    FINALIZERS="--g-finalization yes"
else
    AC_MSG_RESULT([no])
    FINALIZERS="--g-finalization no"
fi
AC_SUBST(FINALIZERS)

# Android checks
if test x$fiji_android = xyes; then
    ANDROID_CPPFLAGS="-D_ANDROID_LIB_ -I$ANDROID/hardware/libhardware/include -I$ANDROID/system/core/include"
    ANDROID_LDFLAGS="-L$ANDROID/out/target/product/$ANDROIDPRODUCT/obj/lib -lsupc++"

    XTRA_CPPFLAGS="$XTRA_CPPFLAGS $ANDROID_CPPFLAGS"
    CPPFLAGS="$CPPFLAGS $ANDROID_CPPFLAGS"
    XTRA_LDFLAGS="$XTRA_LDFLAGS $ANDROID_LDFLAGS"
    LDFLAGS="$LDFLAGS $ANDROID_LDFLAGS"
fi

# Architecture tests for the compiler -- these should come before most
# everything else.
ARCHCFLAGS="$XTRA_CFLAGS"

AC_MSG_CHECKING([for system pointer size (bytes)])
if test x$PTRSIZE = x; then
    # Cheat a little bit -- use the error return value as the pointer size
    AC_RUN_IFELSE(AC_LANG_SOURCE([[
int main (int argc, char *argv[])
{
    return sizeof(void *);
}
]]),
                  [],
                  [PTRSIZE=$?],
                  [# Cross-compile default
                   case $host_cpu in
                   i?86)
                       PTRSIZE=4
                       ;;
                   x86_64)
                       PTRSIZE=8
                       ;;
                   *)
                       PTRSIZE=4
                       fiji_warn_cross=yes
                       fiji_warn_ptrsize=yes
                       ;;
                   esac])
fi
AC_MSG_RESULT([$PTRSIZE])
AC_SUBST(PTRSIZE)

case $PTRSIZE in
4)
   AC_DEFINE(FIVMSYS_SYS_PTRSIZE,4,[system pointer size])
   FIJI_COMPILE_OPT([ARCHCFLAGS], [-m32])
   case $host_cpu in
   i?86)
       FIJI_COMPILE_OPT([ARCHCFLAGS], [-m32 -march=i686])
       ;;
   esac
   ;;
8)
   AC_DEFINE(FIVMSYS_SYS_PTRSIZE,8,[system pointer size])
   FIJI_COMPILE_OPT([ARCHCFLAGS], [-m64])
   case $host_cpu in
   x86_64)
       FIJI_COMPILE_OPT([ARCHCFLAGS], [-march=nocona -m64])
       ;;
   esac
   ;;
*)
   AC_MSG_ERROR([bad pointer size])
   ;;
esac

case $host_os in
rtems*)
  case $host_cpu in
    sparc*)
       FIJI_COMPILE_OPT([ARCHCFLAGS], [-msoft-float -mcpu=v7])
       ;;
  esac
esac

CFLAGS="$ARCHCFLAGS $CFLAGS"

AC_MSG_CHECKING([if pointer size set correctly])
AC_RUN_IFELSE(AC_LANG_SOURCE([[
int main(int c,char **v) {
   if (sizeof(void*)==FIVMSYS_SYS_PTRSIZE) {
      return 0;
   } else {
      return 1;
   }
}]]),[
    AC_MSG_RESULT(yes)
],[
    AC_MSG_ERROR([failed to set pointer size])
],[
    AC_MSG_RESULT(could not determine)
])

fiji_have_sem_init=no

AC_SEARCH_LIBS([floor], [m], [], [AC_MSG_ERROR([Cannot find libm])])
if test $fiji_osflavor = POSIX; then
    AC_SEARCH_LIBS([dlopen], [dl], [],
                   [if test x$fiji_dyn_loading =  xyes; then fiji_dyn_loading=no; fi])
    AC_SEARCH_LIBS([iconv], [iconv], [AC_DEFINE(HAVE_ICONV,1,[foo])])
    AC_SEARCH_LIBS([pthread_create],[pthread],[],
	           [AC_MSG_ERROR([Cannot find pthreads])])
    AC_SEARCH_LIBS([sem_init],[pthread rt],[fiji_have_sem_init=yes])
    AC_SEARCH_LIBS([clock_gettime],[pthread rt],
    		   [AC_DEFINE([HAVE_CLOCK_GETTIME], [1])])
fi

# OS X has sem_init, but it doesn't work.  Here we check whether sem_init
# actually DOES anything.
AC_MSG_CHECKING([if sem_init() works])
if test x$fiji_force_sem_init = xyes; then
    AC_MSG_RESULT([yes (forced)])
    AC_DEFINE([HAVE_WORKING_SEM_INIT], [1], [Defined if sem_init() works.])
elif test x$fiji_have_sem_init = xyes; then
    AC_RUN_IFELSE(AC_LANG_SOURCE([[
#include <semaphore.h>
#define TESTVAL 5

int main(int argc, char *argv[]) {
    sem_t sem;
    int res, val;
    res = sem_init(&sem, 0, TESTVAL);
    return res == -1;
}]]),
                  [AC_MSG_RESULT([yes])
                   AC_DEFINE([HAVE_WORKING_SEM_INIT], [1])],
                  [AC_MSG_RESULT([no])],
                  [fiji_warn_cross=yes
                   fiji_warn_sem_init=yes
                   AC_MSG_RESULT([yes (guessing)])
                   AC_DEFINE([HAVE_WORKING_SEM_INIT], [1])])
else
    AC_MSG_RESULT([no (not present)])
fi

AC_SYS_LARGEFILE

AC_CHECK_HEADERS([inttypes.h stdbool.h stdlib.h stdio.h stdarg.h])
AC_CHECK_HEADERS([alloca.h strings.h sys/mman.h string.h pthread.h])
AC_CHECK_HEADERS([math.h errno.h unistd.h sys/types.h fcntl.h sys/ioctl.h])
AC_CHECK_HEADERS([sys/socket.h sys/un.h netinet/in.h netinet/tcp.h netdb.h])
AC_CHECK_HEADERS([arpa/inet.h dirent.h sys/stat.h ifaddrs.h sys/vfs.h])
AC_CHECK_HEADERS([sys/param.h sys/mount.h time.h sys/ucontext.h setjmp.h])
if test x$fiji_osflavor = xPOSIX; then
    AC_CHECK_HEADERS([semaphore.h signal.h])
fi

# Must come after BOTH OS check and dlopen check
AC_MSG_CHECKING([whether to enable dynamic loading])
if test x$fiji_dyn_loading = xno; then
    NODYNLOADING=--no-dyn-loading
    AC_MSG_RESULT([no])
else
    NODYNLOADING=""
    AC_MSG_RESULT([yes])
fi
AC_SUBST(NODYNLOADING)

AC_MSG_CHECKING(for stat::st_mtime)
AC_LINK_IFELSE(AC_LANG_SOURCE([[
#ifdef HAVE_SYS_TYPES_H
#  include <sys/types.h>
#endif
#ifdef HAVE_SYS_STAT_H
#  include <sys/stat.h>
#endif
#ifdef HAVE_UNISTD_H
#  include <unistd.h>
#endif
int main() {
  struct stat st;
  st.st_mtime = 5;
  if (st.st_mtime == 5) {
    return 0;
  } else {
    return 1;
  }
}
]]),
               [AC_MSG_RESULT(yes)
                AC_DEFINE(HAVE_STAT_MTIME,1,[foo])],
               [AC_MSG_RESULT(no)])

AC_MSG_CHECKING(for stat::st_mtimespec)
AC_LINK_IFELSE(AC_LANG_SOURCE([[
#ifdef HAVE_SYS_TYPES_H
#  include <sys/types.h>
#endif
#ifdef HAVE_SYS_STAT_H
#  include <sys/stat.h>
#endif
#ifdef HAVE_UNISTD_H
#  include <unistd.h>
#endif
int main() {
  struct stat st;
  st.st_mtimespec.tv_nsec = 5;
  if (st.st_mtimespec.tv_nsec == 5) {
    return 0;
  } else {
    return 1;
  }
}
]]),
               [AC_MSG_RESULT(yes)
                AC_DEFINE(HAVE_STAT_MTIMESPEC,1,[foo])],
               [AC_MSG_RESULT(no)])

AC_C_BIGENDIAN([ENDIANNESS=BIG], [ENDIANNESS=LITTLE],
               [AC_MSG_ERROR([Cannot determine if your system is big endian or little endian!])])
AC_SUBST(ENDIANNESS)

AC_MSG_CHECKING(how to prevent inlining)
AC_COMPILE_IFELSE(AC_LANG_PROGRAM([[
    void my_func() __attribute__((noinline));
    void my_func() {
        /* blah */
    }
]]),[
    AC_MSG_RESULT([using __attribute__((noinline))])
    NOINLINEATTR="__attribute__((noinline))"
    AC_DEFINE([NO_INLINE_ATTR],[__attribute__((noinline))],[Attribute to use to prevent inlining])
],[
    AC_MSG_RESULT([don't know])
    NOINLINEATTR=""
    AC_DEFINE([NO_INLINE_ATTR],[],[Attribute to use to prevent inlining])
])
AC_SUBST(NOINLINEATTR)

AC_MSG_CHECKING(how to force inlining)
AC_COMPILE_IFELSE(AC_LANG_PROGRAM([[
    void my_func() __attribute__((always_inline));
    void my_func() {
        /* blah */
    }
]]),[
    AC_MSG_RESULT([using __attribute__((always_inline))])
    AC_DEFINE([FORCE_INLINE_ATTR],[__attribute__((always_inline))],[Attribute to use to force inlining])
],[
    AC_MSG_RESULT([don't know])
    AC_DEFINE([FORCE_INLINE_ATTR],[],[Attribute to use to force inlining])
])

AC_C_INLINE
AC_C_CONST
AC_C_VOLATILE

AC_MSG_CHECKING(for backend architecture)
BACKENDARCH=$fiji_backend_arch
case $BACKENDARCH in
x86|X86)
  BACKENDARCH=X86
  AC_MSG_RESULT(x86)
  ;;
no|none)
  BACKENDARCH=NONE
  AC_MSG_RESULT(none)
  ;;
yes)
  if test x$cross_compiling = xyes; then
    case $host_cpu in
    i?86|x86_64)
      if test x$PTRSIZE = x4; then
        BACKENDARCH=X86
        AC_MSG_RESULT(x86)
      else
        BACKENDARCH=NONE
        AC_MSG_RESULT(none)
      fi
      ;;
    *)
      BACKENDARCH=NONE
      AC_MSG_RESULT(none)
      ;;
    esac
  else
    if test x$PTRSIZE = x4; then
      AC_RUN_IFELSE(AC_LANG_SOURCE([[[
        int main() {
          int tmp=0;
          __asm__ __volatile__ (
            "xorl %%eax, %%eax\n"
            "incl %%eax\n"
            "pushl %%eax\n"
            "popl %0\n"
            : "+r" (tmp)
            :
            : "memory", "cc"
            );
          if (tmp==1) {
            return 0;
          } else {
            return 1;
          }
        }
    	]]]),[
        BACKENDARCH=X86
        AC_MSG_RESULT(x86)
      ],[
        BACKENDARCH=NONE
        AC_MSG_RESULT(none)
      ])
    else
      BACKENDARCH=NONE
      AC_MSG_RESULT(none)
    fi
  fi
  ;;
*)
  AC_MSG_RESULT(unrecognized backend architecture)
  ;;
esac
AC_SUBST(BACKENDARCH)

AC_MSG_CHECKING(for supported synchronization)
if test x$SYNCTYPE = x; then
  if test x$cross_compiling = xyes; then
    case $host_cpu in
    i?86|x86_64)
      SYNCTYPE=x86-gcc
      ;;
    powerpc)
      SYNCTYPE=ppc-gcc
      ;;
    arm*)
      SYNCTYPE=gcc-intrinsic
      ;;
    *)
      case $host_os in
      rtems*)
        SYNCTYPE=rtems
        ;;
      *)
        AC_MSG_ERROR(could not detect synchronization)
        ;;
      esac
      ;;
    esac
  else
    AC_RUN_IFELSE(AC_LANG_SOURCE([[
      #include <inttypes.h>
      /* FIXME: we need this to only pass on x86 */
      static inline void fivmr_cas_void(uintptr_t *ptr,
				        uintptr_t comparand,
				        uintptr_t newValue) {
        __sync_bool_compare_and_swap(ptr,comparand,newValue);
      }
      int main() {
        uintptr_t x=0;
        fivmr_cas_void(&x,0,1);
        if (x==1) {
          return 0;
        } else {
          return 1;
        }
      }
    ]]),
    [SYNCTYPE=gcc-intrinsic],[
      AC_RUN_IFELSE(AC_LANG_SOURCE([[
        #include <inttypes.h>
        static inline void fivmr_cas_void(uintptr_t *ptr,
                                          uintptr_t comparand,
                                          uintptr_t newValue) {
          __asm__ __volatile__ (
            "lock; cmpxchg %2, %1\n"
            : "+a"(comparand),
              "+m"(*ptr)
            : "r"(newValue)
            : "memory", "cc"
            );
        }
        int main() {
          uintptr_t x=0;
          fivmr_cas_void(&x,0,1);
          if (x==1) {
            return 0;
          } else {
            return 1;
          }
        }
      ]]),
      [SYNCTYPE=x86-gcc],[
        AC_RUN_IFELSE(AC_LANG_SOURCE([[
          #include <inttypes.h>
          static inline void fivmr_cas_void(uintptr_t *ptr,
                                            uintptr_t comparand,
                                            uintptr_t newValue) {
            int32_t tmp;
            __asm__ __volatile__ (
                    "1:\n"
                    "lwarx %3,0,%0\n"
                    "cmpw %3,%1\n"
                    "bne- 0f\n"
                    "stwcx. %2,0,%0\n"
                    "bne- 1b\n"
                    "0:\n"
                    : "+r"(ptr),
                      "+r"(comparand),
                      "+r"(newValue),
                      "=r"(tmp)
                    :
                    : "memory", "cr0"
                    );
          }
          int main() {
            uintptr_t x=0;
            fivmr_cas_void(&x,0,1);
            if (x==1) {
              return 0;
            } else {
              return 1;
            }
          }
        ]]),
        [SYNCTYPE=ppc-gcc],[
          case $host_os in
          rtems*)
            SYNCTYPE=rtems
            ;;
          *)
            AC_MSG_ERROR("cannot detect synchronization type")
            ;;
          esac
        ])])])
  fi
fi

if test x$SYNCTYPE = x; then
    if test x$fiji_platform_sync; then
        SYNCTYPE=$fiji_platform_sync
    else
        AC_MSG_ERROR([unrecognized architecture])
    fi
fi

AC_MSG_RESULT($SYNCTYPE)
AC_SUBST(SYNCTYPE)

case $SYNCTYPE in
x86-gcc)
    SYNCTYPEMAC=X86_GCC
    ;;
ppc-gcc)
    SYNCTYPEMAC=PPC_GCC
    ;;
gcc-intrinsic)
    SYNCTYPEMAC=GCC_INTRINSIC
    ;;
rtems)
    SYNCTYPEMAC=RTEMS
    ;;
*)
    AC_MSG_ERROR([INTERNAL: unrecognized sync type])
    ;;
esac
AC_SUBST(SYNCTYPEMAC)

PLATFORMTARGETS=
case $BACKENDARCH in
X86)
   PLATFORMTARGETS="fivmr_asm_x86.o"
   ;;
*)
   PLATFORMTARGETS=
   ;;
esac

AC_SUBST(PLATFORMTARGETS)

OSTARGETS=
case $OSFLAVOR in
RTEMS)
    OSTARGETS=['$(RTEMSTARGS)']
    ;;
POSIX)
    OSTARGETS=['$(POSIXTARGS)']
    ;;
esac

AC_SUBST(OSTARGETS)

# FIXME: the two checks below should use the -gcc naming convention as above,
# so that in the future we can support other types of inline assembly.

AC_MSG_CHECKING(for pause support)
if test x$fiji_pause = x; then
    case $host_cpu in
    i?86|x86_64)
        AC_LINK_IFELSE(AC_LANG_SOURCE([[
int main() {
  __asm__ ("pause\n");
  return 0;
}
]]),
                       [fiji_pause=x86])
        ;;
    esac
fi

case $fiji_pause in
x86)
    AC_DEFINE(HAVE_X86_PAUSE,1,[foo])
    ;;
*)
    fiji_pause=none
    ;;
esac
AC_MSG_RESULT($fiji_pause)
    
AC_MSG_CHECKING(for supported timestamp)
if test x$fiji_timestamp = x; then
    case $host_cpu in
    i?86|x86_64)
        AC_RUN_IFELSE(AC_LANG_SOURCE([[
#include <inttypes.h>
static inline uint64_t fivmr_readCPUTimestamp(void) {
  uint32_t low;
  uint32_t high;
  __asm__ __volatile__ (
	"rdtsc\n"
	: "+a"(low),
	  "+d"(high)
	);
  return low|((uint64_t)high<<32);
}
int main() {
  if (fivmr_readCPUTimestamp()) {
    return 0;
  } else {
    return 1;
  }
}
]]),
                    [fiji_timestamp=x86], [], [fiji_timestamp=cross])
        ;;
    powerpc)
        AC_RUN_IFELSE(AC_LANG_SOURCE([[
#include <inttypes.h>
static inline uint64_t fivmr_readCPUTimestamp(void) {
  uint32_t low;
  uint32_t high;
  __asm__ __volatile__ (
	"mftb %0\n"
	"mftbu %1\n"
	: "+r"(low),
	  "+r"(high)
	);
  return low|((uint64_t)high<<32);
}
int main() {
  if (fivmr_readCPUTimestamp()) {
    return 0;
  } else {
    return 1;
  }
}
]]),
                      [fiji_timestamp=ppc], [], [fiji_timestamp=cross])
        ;;
    esac
fi

case $fiji_timestamp in
x86)
    AC_MSG_RESULT(x86)
    AC_DEFINE(HAVE_X86_RDTSC,1,[foo])
    ;;
ppc)
    AC_MSG_RESULT(ppc)
    AC_DEFINE(HAVE_PPC_MFTB,1,[foo])
    ;;
cross)
    AC_MSG_RESULT(none)
    fiji_warn_cross=yes
    fiji_warn_timestamp=yes
    ;;
*)
    AC_MSG_RESULT(none)
    ;;
esac

AC_MSG_CHECKING([log(page size)])
if test x$LOGPAGESIZE = x; then
    AC_RUN_IFELSE(AC_LANG_PROGRAM([[
    #include <unistd.h>
    #include <stdio.h>
    ]], [[
        int pagesize;
        int logpagesize;
        FILE *fout;
        pagesize = getpagesize();
        if (pagesize<=0) return 1;
        logpagesize = -1;
        while (pagesize>0) {
          pagesize/=2;
          logpagesize++;
        }
        fout=fopen("conftest.out","w");
        if (fout==NULL) return 1;
        fprintf(fout,"%d\n",logpagesize);
        fclose(fout);
    ]]),
                  [LOGPAGESIZE=`cat conftest.out`
                   AC_MSG_RESULT($LOGPAGESIZE)
                   AC_SUBST(LOGPAGESIZE)],
                  [AC_MSG_ERROR([could not determine log(page size)])],
                  [AC_MSG_RESULT([12])
                   LOGPAGESIZE=12
                   fiji_warn_cross=yes
                   fiji_warn_logpagesize=yes])
else
    AC_MSG_RESULT($LOGPAGESIZE)
fi

AC_SUBST(LOGPAGESIZE)

AC_MSG_CHECKING(if priority inheritance mutexes work)
if test x$fiji_pthread_pip = x; then
    AC_RUN_IFELSE(AC_LANG_SOURCE([[
#include <pthread.h>
int main() {
    pthread_mutex_t lock;
    pthread_mutexattr_t attr;
    if (pthread_mutexattr_init(&attr)<0) return 1;
    if (pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_INHERIT)<0) return 1;
    if (pthread_mutex_init(&lock,&attr)<0) return 1;
    if (pthread_mutex_lock(&lock)<0) return 1;
    if (pthread_mutex_unlock(&lock)<0) return 1;
    if (pthread_mutex_destroy(&lock)<0) return 1;
    return 0;
}
]]),
                  [fiji_pthread_pip=yes],
                  [fiji_pthread_pip=no],
                  [fiji_pthread_pip=no
                   fiji_warn_cross=yes
                   fiji_warn_pip=yes])
fi

if test x$fiji_pthread_pip = xyes; then
    AC_MSG_RESULT([yes])
    AC_DEFINE(HAVE_PTHREAD_PIP,1,[foo])
else
    AC_MSG_RESULT([no])
fi

AC_MSG_CHECKING(if priority ceiling mutexes work)
if test x$fiji_pthread_pcep = x; then
    AC_RUN_IFELSE(AC_LANG_SOURCE([[
#include <pthread.h>
int main() {
    pthread_mutex_t lock;
    pthread_mutexattr_t attr;
    if (pthread_mutexattr_init(&attr)<0) return 1;
    if (pthread_mutexattr_setprotocol(&attr,PTHREAD_PRIO_PROTECT)<0) return 1;
    if (pthread_mutexattr_setprioceiling(&attr,sched_get_priority_max(SCHED_FIFO))<0) return 1;
    if (pthread_mutex_init(&lock,&attr)<0) return 1;
    if (pthread_mutex_lock(&lock)<0) return 1;
    if (pthread_mutex_unlock(&lock)<0) return 1;
    if (pthread_mutex_destroy(&lock)<0) return 1;
    return 0;
}
]]),
                  [fiji_pthread_pcep=yes],
                  [fiji_pthread_pcep=no],
                  [fiji_pthread_pcep=no
                   fiji_warn_cross=yes
                   fiji_warn_pcep=yes])
fi

if test x$fiji_pthread_pcep = xyes; then
    AC_MSG_RESULT(yes)
    AC_DEFINE(HAVE_PTHREAD_PCEP,1,[foo])
else
    AC_MSG_RESULT(no)
fi

AC_MSG_CHECKING(for IPv6 support)
if test x$fiji_ipv6 = x; then
    AC_LINK_IFELSE(AC_LANG_SOURCE([[
#ifdef HAVE_NETINET_IN_H
#  include <netinet/in.h>
#endif
int main() {
  struct sockaddr_in6 foo;
  foo.sin6_port = 5;
  if (foo.sin6_port==5) {
    return 0;
  } else {
    return 1;
  }
}
]]),
                   [fiji_ipv6=yes],
                   [fiji_ipv6=no])
fi

if test x$fiji_ipv6 = xyes; then
    AC_MSG_RESULT(yes)
    AC_DEFINE(HAVE_INET6,1,[foo])
else
    AC_MSG_RESULT(no)
fi

# These two don't seem to be used anywhere ...

AC_MSG_CHECKING(for MAP_ANON)
AC_COMPILE_IFELSE(AC_LANG_SOURCE([[
    #include <sys/types.h>
    #include <sys/mman.h>
    #include <stdio.h>
    
    int main() {
        printf("%d\n",MAP_ANON);
        return 0;
    }
]]),[
    AC_MSG_RESULT(yes)
    AC_DEFINE(HAVE_MAP_ANON,1,
              [Defined if the MAP_ANON constant is defined.])
],[
    AC_MSG_RESULT(no)
])

AC_MSG_CHECKING(for MAP_ANONYMOUS)
AC_COMPILE_IFELSE(AC_LANG_SOURCE([[
    #include <sys/types.h>
    #include <sys/mman.h>
    #include <stdio.h>
    
    int main() {
        printf("%d\n",MAP_ANONYMOUS);
        return 0;
    }
]]),[
    AC_MSG_RESULT(yes);
    AC_DEFINE(HAVE_MAP_ANONYMOUS,1,
              [Defined if the MAP_ANONYMOUS constant is defined.])
],[
    AC_MSG_RESULT(no)
])

OPTCFLAGS=""
DEBUGCFLAGS=""
WARNCFLAGS=""
NOWARNCFLAGS=""
SIZECFLAGS=""

# There are circumstances under which gcc will allow options that are
# inappropriate for a given platform, but then fail at compile and/or
# runtime.  Therefore, we've broken these out by platform.  This is
# unfortunate.
#
# FIXME: Make this ... smarter?

# Clang will accept some of these options, but then refuse them later
# depending on other command line options.  (For example, -mfpmath=sse
# works with -lanything, but not without!)  Therefore, we first tell
# it that's not a fatal error.

FIJI_COMPILE_OPT([OPTCFLAGS], [-Wno-unused-command-line-argument])

# These are more-or-less universal
FIJI_COMPILE_OPT([OPTCFLAGS], [-O3])
FIJI_COMPILE_OPT([OPTCFLAGS], [-fno-strict-aliasing])
FIJI_COMPILE_OPT([OPTCFLAGS], [-fno-optimize-sibling-calls])
FIJI_COMPILE_OPT([OPTCFLAGS], [-fomit-frame-pointer])
FIJI_COMPILE_OPT([OPTCFLAGS], [-falign-loops])
FIJI_COMPILE_OPT([OPTCFLAGS], [-fwrapv])

# These aren't universal, but are present on more than one platform
FIJI_COMPILE_OPT([OPTCFLAGS], [-momit-leaf-frame-pointer])

# These are platform-specific and/or problematic on some platforms
case "$host_cpu" in
    i?86|x86_64)
        AC_MSG_CHECKING([whether to try SSE instruction generation])
	if test x$fiji_use_sse != xno; then
	    AC_MSG_RESULT([yes])
	    FIJI_COMPILE_OPT([OPTCFLAGS], [-msse2])
	    FIJI_COMPILE_OPT([OPTCFLAGS], [-mfpmath=sse])
	else
	    AC_MSG_RESULT([no])
	fi
	FIJI_COMPILE_OPT([OPTCFLAGS], [-fno-pic])
        # This is really x86_64-only
        FIJI_COMPILE_OPT([OPTCFLAGS], [-mdynamic-no-pic])
        ;;
    sparc*)
        FIJI_COMPILE_OPT([OPTCFLAGS], [-mno-app-regs])
        ;;
    ppc*)
        ;;
    arm*)
        case "$host" in
        *-gnueabihf)
            FIJI_COMPILE_OPT([OPTCFLAGS], [-mfloat-abi=hard -mfpu=vfp])
        esac
        ;;
esac

FIJI_COMPILE_OPT([SIZECFLAGS], [-Os])

FIJI_COMPILE_OPT([DEBUGCFLAGS], [-g])

FIJI_COMPILE_OPT([WARNCFLAGS], [-W])
FIJI_COMPILE_OPT([WARNCFLAGS], [-Werror])
FIJI_COMPILE_OPT([WARNCFLAGS], [-Wno-unused-parameter])

FIJI_COMPILE_OPT([NOWARNCFLAGS], [-w])

LDFLAGS="$LDFLAGS $ARCHCFLAGS"

AC_SUBST(ARCHCFLAGS)
AC_SUBST(OPTCFLAGS)
AC_SUBST(SIZECFLAGS)
AC_SUBST(DEBUGCFLAGS)
AC_SUBST(WARNCFLAGS)
AC_SUBST(NOWARNCFLAGS)

AC_CHECK_FUNCS([opendir readdir_r closedir])
AC_CHECK_FUNCS([ioctl fcntl bind listen shutdown send recv access])
AC_CHECK_FUNCS([stat unlink realpath mkdir chmod rename statfs])
AC_CHECK_FUNCS([getifaddrs freeifaddrs inet_aton gethostname])
AC_CHECK_FUNCS([recvfrom sendto select socket connect])
AC_CHECK_FUNCS([getsockname getpeername accept fsync])
AC_CHECK_FUNCS([FD_CLR FD_ISSET FD_SET FD_ZERO])
AC_CHECK_FUNCS([get_nprocs getcwd setvbuf sigaction])
AC_CHECK_FUNCS([gethostbyname gethostbyaddr])
AC_CHECK_FUNCS([getsockopt setsockopt setjmp longjmp])

if test x$fiji_osflavor = xPOSIX; then
    AC_CHECK_FUNCS([sched_setscheduler sched_get_priority_min sched_get_priority_max])
    AC_CHECK_FUNCS([pthread_attr_setinheritsched])
    AC_CHECK_FUNCS([pthread_setaffinity_np])
    AC_CHECK_FUNCS([pthread_mutexattr_setprotocol pthread_mutexattr_setprioceiling])
    AC_CHECK_FUNCS([sem_init sem_post sem_wait sem_destroy])
    AC_CHECK_FUNCS([clock_gettime])
fi

AC_MSG_CHECKING(if assembler requires underscores)
echo "void foo(void) {}" > conftest.c
$CC -S conftest.c $CPPFLAGS $CFLAGS
if grep _foo conftest.s > /dev/null; then
  AC_MSG_RESULT(yes)
  ASM_UNDERSCORE=1
else
  AC_MSG_RESULT(no)
  ASM_UNDERSCORE=0
fi
AC_SUBST(ASM_UNDERSCORE)

AC_CHECK_SIZEOF(void *)
AC_CHECK_SIZEOF(size_t)
AC_CHECK_SIZEOF(off_t)
AC_CHECK_SIZEOF(char)
AC_CHECK_SIZEOF(short)
AC_CHECK_SIZEOF(int)
AC_CHECK_SIZEOF(long)
AC_CHECK_SIZEOF(float)
AC_CHECK_SIZEOF(double)
AC_CHECK_SIZEOF(long long)
AC_CHECK_SIZEOF(long double)

AC_CONFIG_HEADERS([fivmr_target.h])
AC_CONFIG_FILES([fivmcrc Rules fivmc_def_config.h fivmr_asm_defs.h debug/Makefile debug/fivmc_tag_dep_config.h opt/Makefile opt/fivmc_tag_dep_config.h small/Makefile small/fivmc_tag_dep_config.h target-results.conf])

AC_OUTPUT

if test x$fiji_warn_cross = xyes; then
    cat 1>&2 <<EOF

WARNING

This target is configured for cross-compile, but we could not verify all
system parameters.  You should check that the following parameters are
correct:

EOF
    if test x$fiji_warn_ptrsize = xyes; then
        cat 1>&2 <<EOF
--enable-ptrsize=$PTRSIZE
    The pointer size for your target system is unknown.  You may wish to
    provide an argument to --enable-ptrsize.

EOF
    fi
    if test x$fiji_warn_timestamp = xyes; then
        cat 1>&2 <<EOF
--enable-timestamp=none
    The timestamp mechanism cannot be guessed for cross-compiling.  You
    may wish to provide an argument to --enable-timestamp.

EOF
    fi
    if test x$fiji_warn_logpagesize = xyes; then
        cat 1>&2 <<EOF
--enable-log-pagesize=$LOGPAGESIZE
    We are cross-compiling, but --with-log-pagesize was not provided.

EOF
    fi
    if test x$fiji_osflavor = xPOSIX -a x$fiji_warn_pip = xyes; then
        cat 1>&2 <<EOF
--disable-pthread-pip
    Priority inheritance mutexes cannot be guessed for cross-compiling.
    You may wish to provide --enable-pthread-pip if your platform
    supports them.

EOF
    fi
    if test x$fiji_osflavor = POSIX -a x$fiji_warn_pcep = xyes; then
        cat 1>&2 <<EOF
--disable-pthread-pcep
    Priority ceiling mutexes cannot be guessed for cross-compiling.
    You may wish to provide --enable-pthread-pcep if your platform
    supports them.

EOF
    fi
    if test x$fiji_osflavor = xPOSIX -a x$fiji_warn_sem_init = xyes; then
        cat 1>&2 <<EOF
--enable-sem-init
    The target platform has sem_init(), but configure was unable to
    verify that it works.  You may wish to provide --disable-sem-init
    if sem_init() is broken on your platform (e.g., Darwin).
EOF
    fi
fi
