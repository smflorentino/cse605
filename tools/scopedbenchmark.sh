#/bin/bash
#Specify Fiji Home in your environment variables, please! Or I guess you could here.
#FIJI_HOME=/home/scottflo/repos/cse605/fivm
#Specify the Java compiler to use
FIJI_JAVA_COMPILER=$FIJI_HOME/ecj/ecj

#Location of Fiji Libraries
FIJI_LIB_DIR=$FIJI_HOME/lib

#Classpath for Eclipse Java Compiler
ECJ_CLASSPATH=$FIJI_LIB_DIR/rtsj.jar:$FIJI_LIB_DIR/fivmcommon.jar:$FIJI_LIB_DIR/fivmr.jar:$FIJI_LIB_DIR/fijiscj.jar:$FIJI_LIB_DIR/fivmtest.jar

#Classpath for Fiji Compiler
FIVMC_CLASSPATH=$FIJI_LIB_DIR/rtsj.jar

#Specify the Source Directory(ies) for our Java Programs
HARDRTJSRC=./rtsjsample/src

echo "Compiling Java to Bytecode..."
rm -rf src/build
mkdir src/build

$FIJI_JAVA_COMPILER -Xlint:unchecked -Xlint:deprecated -source 1.5 -target 1.5 -classpath $ECJ_CLASSPATH $HARDRTJSRC -d src/build
echo "Done!"

#Compile Class Files to Native Code
#Specify a classpath for fivmc, if needed 
FIVM_LIB_DIR=$FIJI_HOME/lib
JARS=$FIVM_LIB_DIR/fivmtest.jar
JARS+=" "
#JARS+=$FIVM_LIB_DIR/rtsj.jar
echo "Compiling Bytecode to Native Code..."
fivmc --jobs 4 --g-def-max-mem 500M --g-scoped-memory -o scopeheapCMR src/build/common/*.class src/build/benchmarks/scopes/*.class -m benchmarks/scopes/ScopedHeap
fivmc --jobs 4 --g-scoped-memory --gc HF -o scopeheapHF src/build/common/*.class src/build/benchmarks/scopes/*.class -m benchmarks/scopes/ScopedHeap
fivmc --jobs 4 --g-scoped-memory -o scopeum src/build/common/*.class src/build/benchmarks/scopes/*.class  -m benchmarks/scopes/ScopedUnmanaged
echo "Done!"

