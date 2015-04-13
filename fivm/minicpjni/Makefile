SYSNAME=`../bin/fivmc --std-rc $$FIVMCRC --sys-name`

all_:
	$(MAKE) all

%::
	mkdir -p build-$(SYSNAME)
	echo "CC=`../bin/fivmc --std-rc $$FIVMCRC --cc`" > build-$(SYSNAME)/Makefile.conf
	echo "CPPFLAGS=`../bin/fivmc --std-rc $$FIVMCRC --cppflags`" >> build-$(SYSNAME)/Makefile.conf
	echo "CFLAGS=`../bin/fivmc --std-rc $$FIVMCRC --cflags` -Wall" >> build-$(SYSNAME)/Makefile.conf
	echo "SYSNAME=$(SYSNAME)" >> build-$(SYSNAME)/Makefile.conf
	echo "--jni-on-load JCL_OnLoad" > build-$(SYSNAME)/minicpjnirc
	echo "--jni-on-load VMChannel_OnLoad" >> build-$(SYSNAME)/minicpjnirc
	echo "--jni-on-load VMFile_OnLoad" >> build-$(SYSNAME)/minicpjnirc
	echo "--link-internal minicpjni/build-$(SYSNAME)/minicp_common.o" >> build-$(SYSNAME)/minicpjnirc
	echo "--link-internal minicpjni/build-$(SYSNAME)/minicp_cpio.o" >> build-$(SYSNAME)/minicpjnirc
	echo "--link-internal minicpjni/build-$(SYSNAME)/minicp_vmchannel.o" >> build-$(SYSNAME)/minicpjnirc
	echo "--link-internal minicpjni/build-$(SYSNAME)/minicp_vmfile.o" >> build-$(SYSNAME)/minicpjnirc
	(cd build-`../bin/fivmc --std-rc $$FIVMCRC --sys-name` && $(MAKE) -f ../Makefile.sub $@)
