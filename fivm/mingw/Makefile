#
# Makefile for fivm
# Copyright (c) 2009 Fiji Systems LLC
#

JAVA=java
JAR=jar
RUBY=ruby
TARGETS=Win32
TARGETCONFIGFLAGS=
TARGETALIAS=$(TARGET)

FIJI_JAVA_COMPILER=ruby ecj/ecj.rb

FIJI_LIBRARY_SUFFIX=.dll
FIJI_JNILIB_SUFFIX=.dll

FIVMCFLAGS=

all: fivmcommon.jar.phony fijirt.jar.phony fijicore.jar.phony fivmc.jar.phony fijiscj.jar.phony fivmr.jar.phony hardrtj.jar.phony rtsj.jar.phony fivmutil.jar.phony
	@echo
	@echo "Fiji VM is compiled.  You may use it without installing by executing bin/fivmc,"
	@echo "or you may wish to install it by logging in as root and running 'make install'."
	@echo
	@echo "Enabled libraries:"
	@echo "       FijiCore  -- yes"
	@echo "  GNU Classpath  -- no"
	@echo

clean:
	rm -rf lib/fivmcommon.jar lib/fivmc.jar lib/fivmr.jar lib/fivmtest.jar
	rm -rf common/build compiler/build runtimej/build test/build
	rm -rf testnogc-nogc* testnogc-cmrgc* simplegctest-cmrgc*
	rm -rf hardrtj/build lib/hardrtj.jar lib/cpruntime.jar cpruntime/build
	rm -rf lib/fijicore.jar fijicore/build fijicore/tools/build
	rm -rf fijicore/generated fijirt/build lib/fijirt.jar lib/rtsj.jar

distclean: clean classpath-clean
	rm -rf config.status config.log autom4te.cache Makefile lib/config.rb bin/fivmc_trampoline common/src/com/fiji/fivm/Config.java runtimec/src/fivmr_config.h
	rm -rf target/config.status target/config.log target/autom4te.cache target/fivmr_target.h target/fivmcrc
	rm -rf `ls -d lib/targets/* | grep -v '^lib/targets/null$$'`
	rm -rf runtimec/src/TAGS
	rm -f bin/fivm_ecj

classpath-clean:
	rm -f lib/glibj.zip
	rm -f lib/libjava*$(FIJI_JNILIB_SUFFIX)

maintainer-clean: distclean
	rm -rf configure target/configure target/aclocal.m4 target/fivmr_target.h.in

tags:
	cd runtimec/src && etags *.c *.h

fivmutil.jar.phony:
	rm -rf util/java/build
	mkdir util/java/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath "lib\\fiji-asm-1.0.jar;lib\\fijirt.jar;lib\\fivmcommon.jar;lib\\fivmc.jar;lib\\fast-md5.jar;lib\\fivmr.jar"  `find util/java/src | grep \\\\.java$$` -d util/java/build
	$(JAR) cf lib/fivmutil.jar -C util/java/build .

fivmcommon.jar.phony:
	rm -rf common/build
	mkdir common/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath "lib\\fiji-asm-1.0.jar;lib\\fast-md5.jar"  `find common/src | grep \\\\.java$$` -d common/build
	$(JAR) cf lib/fivmcommon.jar -C common/build .

fijicore.jar.phony:
	rm -rf fijicore/tools/build
	rm -rf fijicore/generated
	rm -rf fijicore/build
	mkdir fijicore/build
	$(FIJI_JAVA_COMPILER) -nowarn -source 1.5 -target 1.5 -bootclasspath "lib\\fivmcommon.jar;lib\\fijirt.jar;fijicore\\build"  `find fijicore/src | grep \\\\.java$$` -d fijicore\\build
	$(JAR) cf lib\\fijicore.jar -C fijicore\\build .

fijiscj.jar.phony:
	rm -rf scjruntime/build
	mkdir scjruntime/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -warn:-serial -source 1.5 -target 1.5 `find scjruntime/src | grep \\\\.java$$` -d scjruntime\\build
	$(JAR) cf lib\\fijiscj.jar -C scjruntime\\build .

fivmr.jar.phony:
	rm -rf runtimej/build
	mkdir runtimej/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -warn:-serial -source 1.5 -target 1.5 -bootclasspath "lib\\fiji-asm-1.0.jar;lib\\fijicore.jar;lib\\fijirt.jar;lib\\fivmcommon.jar;lib\\fijiscj.jar"  `find runtimej/src | grep \\\\.java$$` -d runtimej\\build
	$(JAR) cf lib\\fivmr.jar -C runtimej\\build .

fijirt.jar.phony:
	rm -rf fijirt/build
	mkdir fijirt/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath lib\\fivmcommon.jar  `find fijirt/src | grep \\\\.java$$` -d fijirt\\build
	$(JAR) cf lib\\fijirt.jar -C fijirt\\build .

fivmc.jar.phony:
	rm -rf compiler/build
	mkdir compiler/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath "lib\\fiji-asm-1.0.jar;lib\\fast-md5.jar;lib\\fijirt.jar;lib\\fivmcommon.jar" `find compiler/src | grep \\\\.java$$` -d compiler\\build
	$(JAR) cf lib\\fivmc.jar -C compiler\\build .

fivmtest.jar.phony:
	rm -rf test/build
	mkdir test/build
	(cd test/build && $(JAVA) -jar ..\\..\\lib\\jasmin.jar `find ../src | grep \\\\.j$$`)
	$(FIJI_JAVA_COMPILER) -nowarn -source 1.5 -target 1.5 -classpath "lib\\fivmr.jar;lib\\fijirt.jar;lib\\fivmcommon.jar;lib\\fijicore.jar;test\\build" `find test/src | grep \\\\.java$$` -d test\\build
	rm test/build/com/filpizlo/fivm/test/ClassWeDelete.class
	$(JAR) cf lib\\fivmtest.jar -C test\\build .

hardrtj.jar.phony:
	rm -rf hardrtj/build
	mkdir hardrtj/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath "lib\\fijicore.jar;lib\\fivmr.jar;lib\\fivmcommon.jar;hardrtj\\build" `find hardrtj/src | grep \\\\.java$$` -d hardrtj\\build
	$(JAR) cf lib\\hardrtj.jar -C hardrtj\\build .

rtsj.jar.phony:
	rm -rf RTSJ/build
	mkdir RTSJ/build
	$(FIJI_JAVA_COMPILER) -Xlint:unchecked -Xlint:deprecated -warn:-serial -source 1.5 -target 1.5 -bootclasspath "lib\\fivmr.jar;lib\\fijicore.jar;lib\\fijirt.jar;lib\\fivmcommon.jar" `find RTSJ/src | grep \\\\.java$$` -d RTSJ/build
	$(JAR) cf lib\\rtsj.jar -C RTSJ\\build .

check: check-hfgc check-cmrgc check-cmrgc-fragmented check-cmrgc-disk check-nogc check-cmrgc-poisoned
	echo ALL TESTS PASSED

check-nogc: check-testnogc-nogc
check-cmrgc: check-testnogc-cmrgc-noopt check-testnogc-cmrgc check-testnogc-cmrgc-moreopt check-simplegctest-cmrgc check-simplegctest-cmrgc-selfman
check-cmrgc-disk: check-testnogc-cmrgc-disk
check-cmrgc-poisoned: check-testnogc-cmrgc-poisoned check-simplegctest-cmrgc-poisoned
check-cmrgc-fragmented: check-testnogc-cmrgc-fragmented-forcearraylets check-simplegctest-cmrgc-fragmented-forcearraylets check-testnogc-cmrgc-fragmented check-simplegctest-cmrgc-fragmented
check-hfgc: check-testnogc-hfgc-noopt check-testnogc-hfgc check-simplegctest-hfgc-noopt check-simplegctest-hfgc

check-testnogc-nogc: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-nogc lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G none
	./testnogc-nogc

check-testnogc-cmrgc-disk: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc-disk lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr --code-repo disk
	./testnogc-cmrgc-disk

check-testnogc-cmrgc-noopt: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc-noopt lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr --no-opt
	./testnogc-cmrgc-noopt

check-testnogc-cmrgc: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr
	./testnogc-cmrgc

check-testnogc-hfgc-noopt: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-hfgc-noopt lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G hf --no-opt
	./testnogc-hfgc-noopt

check-testnogc-hfgc: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-hfgc lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G hf
	./testnogc-hfgc

check-testnogc-cmrgc-moreopt: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc-moreopt lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr --more-opt
	./testnogc-cmrgc-moreopt

check-simplegctest-hfgc-noopt: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-hfgc-noopt lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G hf --no-opt
	FIVMR_GC_MAX_MEM=1g FIVMR_GC_TRIGGER=10M ./simplegctest-hfgc-noopt 10 10000000 10
	FIVMR_GC_MAX_MEM=1g FIVMR_GC_TRIGGER=200M ./simplegctest-hfgc-noopt 10 10000000 10
	FIVMR_GC_MAX_MEM=500m FIVMR_GC_TRIGGER=10M ./simplegctest-hfgc-noopt 10 10000000 10

check-simplegctest-hfgc: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-hfgc lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G hf
	FIVMR_GC_MAX_MEM=1g FIVMR_GC_TRIGGER=10M ./simplegctest-hfgc 10 10000000 10
	FIVMR_GC_MAX_MEM=1g FIVMR_GC_TRIGGER=200M ./simplegctest-hfgc 10 10000000 10
	FIVMR_GC_MAX_MEM=500m FIVMR_GC_TRIGGER=10M ./simplegctest-hfgc 10 10000000 10

check-simplegctest-cmrgc: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-cmrgc lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G cmr
	FIVMR_GC_MAX_MEM=800m FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc 10 10000000 10
	FIVMR_GC_MAX_MEM=800m FIVMR_GC_TRIGGER=200M ./simplegctest-cmrgc 10 10000000 10
	FIVMR_GC_MAX_MEM=250M FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc 10 10000000 10

check-simplegctest-cmrgc-selfman: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-cmrgc-selfman lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G cmr --g-self-man-mem
	FIVMR_GC_MAX_MEM=500M FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-selfman 10 10000000 10
	FIVMR_GC_MAX_MEM=500M FIVMR_GC_TRIGGER=200M ./simplegctest-cmrgc-selfman 10 10000000 10
	FIVMR_GC_MAX_MEM=250M FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-selfman 10 10000000 10

check-testnogc-cmrgc-poisoned: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc-poisoned lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr --header-model poisoned
	./testnogc-cmrgc-poisoned

check-simplegctest-cmrgc-poisoned: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-cmrgc-poisoned lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G cmr --header-model poisoned
	FIVMR_GC_MAX_MEM=1G FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-poisoned 10 10000000 10
	FIVMR_GC_MAX_MEM=1G FIVMR_GC_TRIGGER=200M ./simplegctest-cmrgc-poisoned 10 10000000 10
	FIVMR_GC_MAX_MEM=250M FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-poisoned 10 10000000 10

check-testnogc-cmrgc-fragmented: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc-fragmented lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr --object-model fragmented
	./testnogc-cmrgc-fragmented

check-simplegctest-cmrgc-fragmented: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-cmrgc-fragmented lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G cmr --object-model fragmented
	FIVMR_GC_MAX_MEM=1G FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-fragmented 10 10000000 10
	FIVMR_GC_MAX_MEM=1G FIVMR_GC_TRIGGER=200M ./simplegctest-cmrgc-fragmented 10 10000000 10
	FIVMR_GC_MAX_MEM=450M FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-fragmented 10 10000000 10

check-testnogc-cmrgc-fragmented-forcearraylets: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o testnogc-cmrgc-fragmented-forcearraylets lib/fivmtest.jar -m com/filpizlo/fivm/test/TestNoGCMain -G cmr --object-model fragmented --force-arraylets
	./testnogc-cmrgc-fragmented-forcearraylets

check-simplegctest-cmrgc-fragmented-forcearraylets: fivmtest.jar.phony
	$(RUBY) bin/fivmc $(FIVMCFLAGS) --sanity-check 1 -o simplegctest-cmrgc-fragmented-forcearraylets lib/fivmtest.jar -m com/filpizlo/fivm/test/SimpleGCTestMain -G cmr --object-model fragmented --force-arraylets
	FIVMR_GC_MAX_MEM=1G FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-fragmented-forcearraylets 10 10000000 10
	FIVMR_GC_MAX_MEM=1G FIVMR_GC_TRIGGER=200M ./simplegctest-cmrgc-fragmented-forcearraylets 10 10000000 10
	FIVMR_GC_MAX_MEM=500M FIVMR_GC_TRIGGER=10M ./simplegctest-cmrgc-fragmented-forcearraylets 10 10000000 10

doc: javadoc
	cp README doc/README.txt
	cp LEGAL doc/LEGAL.txt
	$(RUBY) bin/fivmc --dollar-zero fivmc --target null --more-help > doc/fivmc_usage.txt

javadoc: fijicore-javadoc

fijicore-javadoc:
	(cd fijicore/src && javadoc -d ../javadoc -classpath ../../lib/fivmcommon.jar:. -public java.lang java.lang.annotation java.lang.reflect java.lang.ref java.io java.net java.nio java.nio.channels java.nio.channels.spi java.nio.charset java.util); \
	if test $$? != 0; then \
	    echo "WARNING: javadoc documentation could not be built" 1>&2; \
	    rm -rf fijicore/javadoc; \
	    mkdir fijicore/javadoc; \
	    echo "<html><head></head><body>This distribution contains no documentation.</body></html>" > fijicore/javadoc/index-all.html; \
	fi
