SRCDIR=../../../..
RTSRCDIR=$(SRCDIR)/runtimec/src
CPPFLAGS=-DHAVE_CONFIG_H   -D_REENTRANT -D_GNU_SOURCE -I.. -I. -I$(RTSRCDIR)
CFLAGS= -m64 -march=nocona -m64  -Wno-unused-command-line-argument -O3 -fno-strict-aliasing -fno-optimize-sibling-calls -fomit-frame-pointer -falign-loops -fwrapv -momit-leaf-frame-pointer -msse2 -mfpmath=sse -fno-pic  -W -Werror -Wno-unused-parameter
LDFLAGS=   -m64 -march=nocona -m64
LIBS=-lpthread -ldl -lm 

TAG=opt

include ../Rules
