#/bin/bash
#Specify the Java compiler to use
FIJI_JAVA_COMPILER=$FIJI_HOME/ecj/ecj
#Location of Fiji Libraries
FIJI_LIB_DIR=$FIJI_HOME/lib
#Classpath for Eclipse Java Compiler
ECJ_CLASSPATH=$FIJI_LIB_DIR/rtsj.jar:$FIJI_LIB_DIR/fivmcommon.jar:$FIJI_LIB_DIR/fivmr.jar:$FIJI_LIB_DIR/fijiscj.jar:$FIJI_LIB_DIR/fivmtest.jar
#Classpath for Fiji Compiler
FIVMC_CLASSPATH=$FIJI_LIB_DIR/rtsj.jar
#Specify the Source Directories for our Java Programs
HARDRTJSRC=/home/scottflo/repos/cse605/tools/src
HARDRTJSRC2=/home/scottflo/repos/cse605/tools/rtsjsample/src
#$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath lib/fijicore.jar:lib/fivmr.jar:lib/fivmcommon.jar:hardrtj/build:lib/fast-md5.jar $HARDRTJSRC -d src/build
echo "Compiling Java to Bytecode..."
#$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath $ECJ_CLASSPATH $HARDRTJSRC -d src/build

$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath $ECJ_CLASSPATH $HARDRTJSRC2 -d src/build

#$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath /home/scottflo/repos/cse605/fivm/lib/rtsj.jar:/home/scottflo/repos/cse605/fivm/lib/fivmcommon.jar $HARDRTJSRC2 -d src/build
echo "Done!"


#Compile Class Files to Native Code 
FIVM_LIB_DIR=$FIJI_HOME/lib
JARS=$FIVM_LIB_DIR/fivmtest.jar
JARS+=" "
#JARS+=$FIVM_LIB_DIR/rtsj.jar
echo "Compiling Bytecode to Native Code..."
#echo "FijiVM Lib: " $FIVM_LIB_DIR
#echo "CP: " $JARS
#fivmc --g-scoped-memory -o myprog -m com/fiji/fivm/test/RawScopedMemoryTest $JARS
#fivmc  -o myprog2 -m com/fiji/fivm/test/GetIntFieldTest $JARS
#fivmc -o hello src/build/test/Hello.class
fivmc --jobs 2 --g-scoped-memory -o hello src/build/common/*.class src/build/test1/*.class --no-opt -m test1/FibMain
#fivmc --jobs 2 --g-scoped-memory -o hello $JARS src/build/common/*.class src/build/test1/*.class --no-opt -m test1/FibExample
#fivmc --jobs 2 --g-scoped-memory --g-def-immortal-mem 1024K --rt-library RTSJ -o hello $FIVMC_CLASSPATH src/build/test2/*.class --no-opt --rt-flowlog-enable
#fivmc -o hello src/build/test/HelloArray.class src/build/test/Kaymar.class --no-opt --rt-flowlog-enable
echo "Done!"
