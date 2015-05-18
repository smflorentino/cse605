# cse605
This is the code repository for:
Group 3 11am-12pm Friday
Janhavi Patil 
Mihir Mehta 
Shreyas Chavan 
Scott Florentino
 
Project 1 : Malloc and free in scoped memory

Stuff you might find useful:

fivm/ - the directory that has the Fiji VM, with our changes. Build it in the usual way.

tools/ - the directory that has our build scripts, tests, and output. Run tests.sh to build the executables that run our tests, and benchmark.sh to run the tools to run our benchmarks for matrix multiplication. Run scopedbenchmarks.sh to run our benchmarks that tests the overhead of creating/entering scopes vs. our implemenation, which avoids that.

tools/jvmjava - a Java project that runs the JVM portion of the Matrix Multiplication benchmark. Use this to compare the performance of the Fiji VM with a regular Java VM.

tools/rtsjsample - a Java project which contains our benchmarking source code; the scripts above use this to build their binaries.

NOTE: 

The scripts in the tools directory require you to set the environment variable of FIJI_HOME be the location of the fivm directory...so if you extracted our assignment to /home/username/cse605, FIJI_HOME would be set to /home/username/cse605/fivm
Alternatively, you can set it in the script.

NOTE:
You might need to reconfigure SCJ, which is inside the fivm/ directory. Run its configure script if you have trouble building.
