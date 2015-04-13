_all:
	$(MAKE) all

checkdeps:
	echo "# generated file -- do not edit" > depend.mk
	echo "SRCFILES=\\" >> depend.mk
	echo `find s3 spec | grep \\\.java$$ | grep -v /ovm/` >> depend.mk

Makefile:
	echo "doing nothing"

%: checkdeps
	echo $@
	rm -f .phony
	./config.status
	$(MAKE) -f RealMakefile $@
