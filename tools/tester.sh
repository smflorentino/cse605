#/bin/bash
#Compile Java File
FIJI_JAVA_COMPILER=$FIJI_HOME/ecj/ecj
FIJI_LIB_DIR=$FIJI_HOME/lib
ECJ_CLASSPATH=$FIJI_LIB_DIR/rtsj.jar:$FIJI_LIB_DIR/fivmcommon.jar
FIVMC_CLASSPATH=$FIJI_LIB_DIR/rtsj.jar
HARDRTJSRC=/home/scottflo/repos/cse605/tools/src
HARDRTJSRC2=/home/scottflo/repos/cse605/tools/rtsjsample/src
#$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath lib/fijicore.jar:lib/fivmr.jar:lib/fivmcommon.jar:hardrtj/build:lib/fast-md5.jar $HARDRTJSRC -d src/build
echo "Compiling Java to Bytecode..."
$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath $ECJ_CLASSPATH $HARDRTJSRC -d src/build

$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath $ECJ_CLASSPATH $HARDRTJSRC2 -d src/build

#$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath /home/scottflo/repos/cse605/fivm/lib/rtsj.jar:/home/scottflo/repos/cse605/fivm/lib/fivmcommon.jar $HARDRTJSRC2 -d src/build
echo "Done!"


#Compile Class File 
FIVM_LIB_DIR=$FIJI_HOME/lib
JARS=$FIVM_LIB_DIR/fivmtest.jar
JARS+=" "
#JARS+=$FIVM_LIB_DIR/rtsj.jar
echo "Compiling Bytecode to Native Code..."
echo "FijiVM Lib: " $FIVM_LIB_DIR
echo "CP: " $JARS
#fivmc --g-scoped-memory -o myprog -m com/fiji/fivm/test/RawScopedMemoryTest $JARS
#fivmc  -o myprog2 -m com/fiji/fivm/test/GetIntFieldTest $JARS
#fivmc -o hello src/build/test/Hello.class
fivmc -o hello $FIVMC_CLASSPATH src/build/test2/FibExample.class --no-opt --rt-flowlog-enable
#fivmc -o hello src/build/test/HelloArray.class src/build/test/Kaymar.class --no-opt --rt-flowlog-enable
echo "Done!"
