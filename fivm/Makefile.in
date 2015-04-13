_all:
	$(MAKE) all

checkdeps:
	echo "# generated file - do not edit" > depend.mk
	echo "FIJIASMSRC=\\" >> depend.mk
	echo `find fiji-asm/src | grep \\\.java$$ | grep -v xml | grep -v optimizer` >> depend.mk
	echo "BOTTOMUPSRC=\\" >> depend.mk
	echo `find bottomup/src | grep \\\.java$$` >> depend.mk
	echo "FIVMCOMMONDEPSRC=\\" >> depend.mk
	echo "common/src/com/fiji/fivm/Config.java.in \\" >> depend.mk
	echo `find common/src | grep \\\\.java$$ | grep -v fivm/Config.java` >> depend.mk
	echo "FIVMCOMMONSRC=\\" >> depend.mk
	echo `find common/src | grep \\\\.java$$` >> depend.mk
	echo "FIJICORESRC=\\" >> depend.mk
	echo `find fijicore/src | grep \\\\.java$$` >> depend.mk
	echo "FIVMRSRC=\\" >> depend.mk
	echo `find runtimej/src | grep \\\\.java$$` >> depend.mk
	echo "CPRUNTIMESRC=\\" >> depend.mk
	echo `find cpruntime/src | grep \\\\.java$$` >> depend.mk
	echo "FIJIRTSRC=\\" >> depend.mk
	echo `find fijirt/src | grep \\\\.java$$` >> depend.mk
	echo "FIVMCSRC=\\" >> depend.mk
	echo `find compiler/src | grep \\\\.java$$` >> depend.mk
	echo "UTILSRC=\\" >> depend.mk
	echo `find util/java/src | grep \\\\.java$$` >> depend.mk
	echo "RUNNERSRC=\\" >> depend.mk
	echo `find runner/src | grep \\\\.java$$` >> depend.mk
	echo "FIVMTESTSRC=\\" >> depend.mk
	echo `find test/java/src | grep \\\\.java$$` >> depend.mk
	echo "FIVMTESTASMSRC=\\" >> depend.mk
	echo `find test/java/src | grep \\\\.j$$` >> depend.mk
	echo "HARDRTJSRC=\\" >> depend.mk
	echo `find hardrtj/src | grep \\\\.java$$` >> depend.mk
	echo "RTSJSRC=\\" >> depend.mk
	echo `find RTSJ/src | grep \\\\.java$$` >> depend.mk
	echo "FIJISCJSRC=\\" >> depend.mk
	echo `find scjruntime/src -name '*.java'` >> depend.mk

Makefile:
	echo "doing nothing"

target-var-check:
	test -n "$(TARGET)" || (echo "You must provide a TARGET= argument to use this command" && exit 1)

target-add: target-var-check

%: checkdeps
	echo $@
	rm -f .phony
	./config.status
	chmod 755 ecj/ecj
	chmod 755 localbin/bottomup
	$(MAKE) -f RealMakefile $@
